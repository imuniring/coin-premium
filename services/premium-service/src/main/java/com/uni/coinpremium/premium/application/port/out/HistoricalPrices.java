package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.premium.domain.MinuteClose;
import java.time.Instant;
import java.util.List;

/**
 * 거래소 과거 1분 종가 조회 (공백 보정용)
 */
public interface HistoricalPrices {

    /**
     * 시작 시각이 [from, to)인 1분 캔들의 종가, 시간 오름차순
     * 체결이 없던 분은 거래소가 캔들을 만들지 않으므로 빠져 있을 수 있다
     */
    List<MinuteClose> minuteCloses(Exchange exchange, String market, Instant from, Instant to);
}