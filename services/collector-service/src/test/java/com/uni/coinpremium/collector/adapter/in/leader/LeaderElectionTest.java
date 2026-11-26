package com.uni.coinpremium.collector.adapter.in.leader;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class LeaderElectionTest {

    @Container
    static final GenericContainer<?> valkey = new GenericContainer<>("valkey/valkey:9.0").withExposedPorts(6379);

    // 테스트에선 기한을 짧게: 0.5초 지나면 열쇠가 저절로 사라짐
    private static final LeaderProperties PROPS = new LeaderProperties(Duration.ofMillis(500), Duration.ofMillis(100));

    private static LettuceConnectionFactory factory;
    private StringRedisTemplate redis;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(valkey.getHost(), valkey.getMappedPort(6379));
        factory.afterPropertiesSet();
        factory.start();
    }

    @AfterAll
    static void disconnect() {
        factory.destroy();
    }

    @BeforeEach
    void setUp() {
        redis = new StringRedisTemplate(factory);
        redis.delete(List.of(LeaderElection.KEY, LeaderElection.RENEWED_AT_KEY));
    }

    @Test
    @DisplayName("두 인스턴스 중 먼저 시도한 한 대만 리더가 된다")
    void onlyOneLeader() {
        Node a = new Node("A");
        Node b = new Node("B");

        a.election.tick();
        b.election.tick();

        assertThat(a.election.isLeader()).isTrue();
        assertThat(b.election.isLeader()).isFalse();
        assertThat(a.granted.get()).isEqualTo(1);   // A만 수집 시작
        assertThat(b.granted.get()).isZero();
    }

    @Test
    @DisplayName("리더가 연장 없이 멈추면(장애) 기한이 지난 뒤 대기조가 넘겨받는다")
    void standbyTakesOverAfterLeaderCrash() throws InterruptedException {
        Node a = new Node("A");
        Node b = new Node("B");
        a.election.tick();   // A 리더

        // A가 쓰러짐 = 더 이상 tick()을 부르지 않음 (연장도, 반납도 없음)
        b.election.tick();
        assertThat(b.election.isLeader()).isFalse();   // 아직 A의 열쇠가 살아 있음

        Thread.sleep(700);   // 기한(0.5초)이 지나 열쇠가 저절로 사라짐
        b.election.tick();

        assertThat(b.election.isLeader()).isTrue();
        assertThat(b.granted.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("열쇠를 다른 인스턴스에 빼앗기면 연장에 실패하고 즉시 수집을 멈춘다 (리더 두 명 방지)")
    void stepsDownWhenLockLost() {
        Node a = new Node("A");
        a.election.tick();   // A 리더

        // A가 멈춰 있는 사이 기한이 지나 B가 열쇠를 가져간 상황을 그대로 만듦
        redis.opsForValue().set(LeaderElection.KEY, "B-instance");

        a.election.tick();   // A가 깨어나 연장 시도 → 내 열쇠가 아님

        assertThat(a.election.isLeader()).isFalse();
        assertThat(a.revoked.get()).isEqualTo(1);   // 수집 중단이 불림
        assertThat(redis.opsForValue().get(LeaderElection.KEY)).isEqualTo("B-instance");   // B의 열쇠는 건드리지 않음
    }

    @Test
    @DisplayName("리더가 정상 종료하면 열쇠를 반납해 대기조가 기한을 기다리지 않고 바로 넘겨받는다")
    void releaseOnShutdown() throws InterruptedException {
        Node a = new Node("A");
        Node b = new Node("B");
        a.election.start();
        Thread.sleep(200);   // A가 첫 시도로 리더가 될 시간
        assertThat(a.election.isLeader()).isTrue();

        a.election.stop();   // 정상 종료: 수집 중단 + 열쇠 반납
        b.election.tick();   // 기한(0.5초)을 기다리지 않고 바로 시도

        assertThat(a.revoked.get()).isEqualTo(1);
        assertThat(b.election.isLeader()).isTrue();
    }

    /** 리더 선출 1대 + 수집 시작·중단 호출 횟수를 세는 가짜 */
    private class Node {
        final AtomicInteger granted = new AtomicInteger();
        final AtomicInteger revoked = new AtomicInteger();
        final LeaderElection election;

        Node(String name) {
            election = new LeaderElection(redis, PROPS, name + "-instance",
                    granted::incrementAndGet, revoked::incrementAndGet);
        }
    }

    @Test
    @DisplayName("대기조가 넘겨받으면 옛 리더가 마지막으로 살아 있던 시각부터의 공백을 기록한다")
    void recordsTakeoverGap() throws InterruptedException {
        Node a = new Node("A");
        Node b = new Node("B");
        a.election.tick();                 // A 리더
        a.election.tick();                 // A 연장 (살아 있던 시각 기록)
        Instant aliveAt = Instant.now();

        Thread.sleep(700);                 // A 쓰러짐 → 기한 만료
        b.election.tick();                 // B 인계

        LeaderElection.TakeoverGap gap = b.election.lastGap();
        assertThat(gap).isNotNull();
        assertThat(gap.from()).isBeforeOrEqualTo(aliveAt);          // A의 마지막 기록부터
        assertThat(gap.duration()).isGreaterThanOrEqualTo(Duration.ofMillis(500));   // 최소 기한만큼은 비었음
    }

    @Test
    @DisplayName("이전 리더 기록이 없으면(최초 기동) 공백을 기록하지 않는다")
    void noGapOnFirstStart() {
        Node a = new Node("A");
        a.election.tick();

        assertThat(a.election.lastGap()).isNull();
    }
}