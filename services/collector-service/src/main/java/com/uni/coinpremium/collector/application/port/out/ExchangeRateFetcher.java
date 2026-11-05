package com.uni.coinpremium.collector.application.port.out;

import com.uni.coinpremium.contract.ExchangeRateEvent;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 환율 조회 포트 (구현: 수출입은행 API 등)
 */
public interface ExchangeRateFetcher {

    /** 해당 날짜의 USD→KRW 환율. 영업일이 아니면 empty */
    Optional<ExchangeRateEvent> fetchUsdKrw(LocalDate date);
}