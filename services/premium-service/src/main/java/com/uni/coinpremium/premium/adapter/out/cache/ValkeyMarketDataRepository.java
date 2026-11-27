package com.uni.coinpremium.premium.adapter.out.cache;

import com.uni.coinpremium.premium.application.exception.MarketDataUnavailableException;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 최신 시세·환율을 Valkey에 저장 (premium 여러 대가 같은 값을 보도록)
 * - quote:{거래소}:{심볼} → Hash { price, tradedAt(epoch ms) }
 * - fx:USD-KRW           → Hash { rate, publishedAt(epoch ms) }
 */
@Repository
public class ValkeyMarketDataRepository implements MarketDataRepository {

    private static final String FX_KEY = "fx:USD-KRW";

    /**
     * 저장된 시각보다 같거나 최신일 때만 교체 (기존 isNewerThan과 같은 규칙)
     * 비교와 교체를 Valkey 안에서 한 번에 실행해, 여러 인스턴스가 동시에 써도 옛날 값이 덮어쓰지 못하게 한다
     * ARGV: [1]시각 필드명 [2]시각(ms) [3]값 필드명 [4]값
     */
    private static final RedisScript<Long> SAVE_IF_NEWER = RedisScript.of("""
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if current and tonumber(current) > tonumber(ARGV[2]) then
              return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public ValkeyMarketDataRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveQuote(PriceQuote quote) {
        call(() -> redis.execute(SAVE_IF_NEWER, List.of(quoteKey(quote.exchange(), quote.symbol())),
                "tradedAt", millis(quote.tradedAt()), "price", quote.price().toPlainString()));
    }

    @Override
    public Optional<PriceQuote> findQuote(String exchange, String symbol) {
        return call(() -> {
            Map<Object, Object> hash = redis.opsForHash().entries(quoteKey(exchange, symbol));
            if (hash.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new PriceQuote(exchange, symbol,
                    new BigDecimal((String) hash.get("price")),
                    Instant.ofEpochMilli(Long.parseLong((String) hash.get("tradedAt")))));
        });
    }

    @Override
    public void saveFxRate(FxRate rate) {
        call(() -> redis.execute(SAVE_IF_NEWER, List.of(FX_KEY),
                "publishedAt", millis(rate.publishedAt()), "rate", rate.rate().toPlainString()));
    }

    @Override
    public Optional<FxRate> findFxRate() {
        return call(() -> {
            Map<Object, Object> hash = redis.opsForHash().entries(FX_KEY);
            if (hash.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new FxRate(
                    new BigDecimal((String) hash.get("rate")),
                    Instant.ofEpochMilli(Long.parseLong((String) hash.get("publishedAt")))));
        });
    }

    /** Valkey 예외(연결 실패·시간 초과)를 서비스가 아는 예외로 바꾼다 */
    private static <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            throw new MarketDataUnavailableException("market data store unavailable: " + e.getClass().getSimpleName(), e);
        }
    }

    private static String quoteKey(String exchange, String symbol) {
        return "quote:" + exchange + ":" + symbol;  // 예: quote:UPBIT:BTC
    }

    private static String millis(Instant instant) {
        return String.valueOf(instant.toEpochMilli());
    }
}
