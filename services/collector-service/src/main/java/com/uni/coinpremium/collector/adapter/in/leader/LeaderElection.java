package com.uni.coinpremium.collector.adapter.in.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

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

    // 열쇠가 내 것일 때만 기한 연장 (확인 + 연장을 한 번에)
    private static final RedisScript<Long> RENEW = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

    // 열쇠가 내 것일 때만 삭제 (정상 종료 시 대기조가 바로 넘겨받게)
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
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

    private boolean acquire() {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY, instanceId, properties.ttl()));
    }

    private boolean renew() {
        Long result = redis.execute(RENEW, List.of(KEY), instanceId, String.valueOf(properties.ttl().toMillis()));
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

    boolean isLeader() {
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
                redis.execute(RELEASE, List.of(KEY), instanceId);   // 대기조가 기한을 기다리지 않고 바로 넘겨받게
            } catch (RuntimeException ex) {
                log.warn("[leader] 종료 시 열쇠 반납 실패 (기한 만료로 해제됨): {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return executor != null;
    }
}
