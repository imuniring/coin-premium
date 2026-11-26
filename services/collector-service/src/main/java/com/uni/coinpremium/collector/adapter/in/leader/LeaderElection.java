package com.uni.coinpremium.collector.adapter.in.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * collector 리더 선출 (active-standby)
 * - 리더 한 대만 거래소에 연결하고, 나머지는 대기하다 리더가 죽으면 넘겨받는다
 * - 열쇠: Valkey leader:collector = {인스턴스 ID}, 기한 ttl
 * - 리더는 renewInterval마다 "열쇠가 내 것일 때만" 기한을 연장하고, 실패하면 즉시 수집을 멈춘다 (리더 두 명 방지)
 */
public class LeaderElection implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LeaderElection.class);
    static final String KEY = "leader:collector";
    static final String RENEWED_AT_KEY = "leader:collector:renewed-at";   // 리더가 마지막으로 살아 있음을 확인한 시각

    // 열쇠가 내 것일 때만 기한 연장 + 살아 있던 시각 기록 (확인·연장·기록을 한 번에)
    // KEYS: [1]열쇠 [2]시각 기록   ARGV: [1]내 ID [2]기한(ms) [3]지금(epoch ms)
    private static final RedisScript<Long> RENEW = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
              redis.call('SET', KEYS[2], ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    // 열쇠가 내 것일 때만 시각 기록 후 삭제 (정상 종료: 공백 시작점을 정확히 남김)
    // KEYS: [1]열쇠 [2]시각 기록   ARGV: [1]내 ID [2]지금(epoch ms)
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              redis.call('SET', KEYS[2], ARGV[2])
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final LeaderProperties properties;
    private final String instanceId;
    private final Runnable onGranted;   // 리더가 됐을 때 (수집 시작)
    private final Runnable onRevoked;   // 리더를 잃었을 때 (수집 중단)

    private volatile ScheduledExecutorService executor;
    private volatile boolean leader;
    private volatile Instant leaseValidUntil = Instant.EPOCH;   // 마지막으로 확인한 열쇠 기한 (Valkey 장애 대비)

    public LeaderElection(StringRedisTemplate redis, LeaderProperties properties, String instanceId, Runnable onGranted, Runnable onRevoked) {
        this.redis = redis;
        this.properties = properties;
        this.instanceId = instanceId;
        this.onGranted = onGranted;
        this.onRevoked = onRevoked;
    }

    // ===== 5초마다 실행 =====

    void tick() {
        Instant before = Instant.now();   // 요청 전 시각 기준으로 기한을 계산해야 보수적 (응답 지연만큼 짧게 잡힘)
        try {
            if (leader) {
                if (renew()) {
                    leaseValidUntil = before.plus(properties.ttl());
                } else {
                    stepDown("열쇠를 잃음 (기한 만료 후 다른 인스턴스가 획득)");
                }
            } else if (acquire()) {
                leaseValidUntil = before.plus(properties.ttl());
                recordTakeoverGap();
                becomeLeader();
            }
        } catch (RuntimeException e) {
            // Valkey에 연결할 수 없으면 열쇠 상태를 확인할 수 없다.
            // 마지막으로 확인한 기한까지만 리더를 유지하고, 지나면 스스로 내려온다 (그 뒤엔 다른 인스턴스가 잡을 수 있으므로)
            log.warn("[leader] Valkey 확인 실패: {}", e.getMessage());
            if (leader && Instant.now().isAfter(leaseValidUntil)) {
                stepDown("Valkey 연결 실패로 열쇠 기한 확인 불가");
            }
        }
    }

    /**
     * 옛 리더가 마지막으로 살아 있던 시각부터 지금까지를 공백으로 기록
     * - 옛 리더는 마지막 기록 후 최대 연장 주기만큼 더 수집했을 수 있어 실제보다 넓게 잡힘 (놓치는 것보다 안전)
     * - 기록이 없으면 최초 기동이라 공백 없음
     */
    private void recordTakeoverGap() {
        Instant now = Instant.now();
        String last = redis.opsForValue().get(RENEWED_AT_KEY);
        redis.opsForValue().set(RENEWED_AT_KEY, String.valueOf(now.toEpochMilli()));
        if (last == null) {
            log.info("[leader] 이전 리더 기록 없음 (최초 기동)");
            return;
        }
        TakeoverGap gap = new TakeoverGap(Instant.ofEpochMilli(Long.parseLong(last)), now);
        lastGap = gap;
        log.atWarn()
                .addKeyValue("from", gap.from())
                .addKeyValue("to", gap.to())
                .addKeyValue("gapMs", gap.duration().toMillis())
                .log("[leader] 리더 교대 공백 - {} ~ {} ({}ms), 공백 보정 대상",
                        gap.from(), gap.to(), gap.duration().toMillis());
    }

    private boolean acquire() {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY, instanceId, properties.ttl()));
    }

    private boolean renew() {
        Long result = redis.execute(RENEW, List.of(KEY, RENEWED_AT_KEY),
                instanceId, String.valueOf(properties.ttl().toMillis()), String.valueOf(Instant.now().toEpochMilli()));
        return result != null && result == 1L;
    }

    private void becomeLeader() {
        leader = true;
        log.info("[leader] 리더 획득 - instanceId={}", instanceId);
        onGranted.run();
    }

    private void stepDown(String reason) {
        leader = false;
        log.warn("[leader] 리더 해제 - instanceId={}, 이유: {}", instanceId, reason);
        onRevoked.run();
    }

    /** 지금 이 인스턴스가 리더인가 (리더만 수집) */
    public boolean isLeader() {
        return leader;
    }

    @Override
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("leader-election").factory());
        executor.scheduleWithFixedDelay(this::tick, 0, properties.renewInterval().toMillis(), TimeUnit.MILLISECONDS);
        log.info("[leader] 리더 선출 시작 - instanceId={}", instanceId);
    }

    @Override
    public void stop() {
        ScheduledExecutorService e = executor;
        if (e == null) {
            return;
        }
        e.shutdownNow();
        executor = null;
        if (leader) {
            stepDown("정상 종료");
            try {
                redis.execute(RELEASE, List.of(KEY, RENEWED_AT_KEY),
                        instanceId, String.valueOf(Instant.now().toEpochMilli()));
            } catch (RuntimeException ex) {
                log.warn("[leader] 종료 시 열쇠 반납 실패 (기한 만료로 해제됨): {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return executor != null;
    }

    /** 리더 교대 공백: 옛 리더가 마지막으로 살아 있던 시각 ~ 새 리더가 수집을 시작한 시각 */
    record TakeoverGap(Instant from, Instant to) {
        Duration duration() {
            return Duration.between(from, to);
        }
    }

    private volatile TakeoverGap lastGap;   // 테스트 확인용

    TakeoverGap lastGap() {
        return lastGap;
    }
}
