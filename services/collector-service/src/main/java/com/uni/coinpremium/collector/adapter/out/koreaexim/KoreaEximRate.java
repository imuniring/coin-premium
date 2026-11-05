package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 수출입은행 환율 응답 1건
 * result: 1=성공, 2=DATA코드 오류, 3=인증코드 오류, 4=일일 제한 횟수 초과
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KoreaEximRate(
        Integer result,
        @JsonProperty("cur_unit") String curUnit,     // "USD"
        @JsonProperty("deal_bas_r") String dealBasR   // 매매기준율 "1,369.4" (쉼표 포함 문자열)
) {}