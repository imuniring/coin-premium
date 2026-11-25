package com.uni.coinpremium.premium.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;
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
class ValkeyMarketDataRepositoryTest {

    @Container
    static final GenericContainer<?> valkey = new GenericContainer<>("valkey/valkey:9.0").withExposedPorts(6379);

    private static final Instant T1 = Instant.parse("2026-11-25T00:00:01Z");
    private static final Instant T2 = Instant.parse("2026-11-25T00:00:02Z");

    private static LettuceConnectionFactory factory;
    private ValkeyMarketDataRepository repository;

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
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.getConnectionFactory().getConnection().serverCommands().flushAll();   // 테스트끼리 데이터가 섞이지 않게
        repository = new ValkeyMarketDataRepository(template);
    }

    @Test
    @DisplayName("더 최신 시세가 오면 교체한다")
    void replaceWithNewer() {
        repository.saveQuote(quote("100", T1));
        repository.saveQuote(quote("200", T2));

        assertThat(repository.findQuote("UPBIT", "BTC")).contains(quote("200", T2));
    }

    @Test
    @DisplayName("순서가 뒤바뀌어 옛날 시세가 와도 최신값을 유지한다")
    void ignoreOlder() {
        repository.saveQuote(quote("200", T2));
        repository.saveQuote(quote("100", T1));   // 늦게 도착한 옛날 시세

        assertThat(repository.findQuote("UPBIT", "BTC")).contains(quote("200", T2));
    }

    @Test
    @DisplayName("같은 시세가 두 번 와도 결과가 같다 (멱등성)")
    void sameQuoteTwice() {
        repository.saveQuote(quote("100", T1));
        repository.saveQuote(quote("100", T1));

        assertThat(repository.findQuote("UPBIT", "BTC")).contains(quote("100", T1));
    }

    @Test
    @DisplayName("환율도 더 최신일 때만 교체한다")
    void fxRateKeepsNewest() {
        repository.saveFxRate(new FxRate(new BigDecimal("1330.8"), T2));
        repository.saveFxRate(new FxRate(new BigDecimal("1300.0"), T1));

        assertThat(repository.findFxRate()).contains(new FxRate(new BigDecimal("1330.8"), T2));
    }

    @Test
    @DisplayName("저장된 값이 없으면 비어 있다")
    void emptyWhenMissing() {
        assertThat(repository.findQuote("UPBIT", "ETH")).isEmpty();
        assertThat(repository.findFxRate()).isEmpty();
    }

    private static PriceQuote quote(String price, Instant tradedAt) {
        return new PriceQuote("UPBIT", "BTC", new BigDecimal(price), tradedAt);
    }
}