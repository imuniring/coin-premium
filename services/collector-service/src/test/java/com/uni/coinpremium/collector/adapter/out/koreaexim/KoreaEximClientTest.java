package com.uni.coinpremium.collector.adapter.out.koreaexim;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KoreaEximClientTest {

    @Test
    @DisplayName("인증서 오류는 TLS 실패로 판단한다")
    void sslHandshakeIsTlsFailure() {
        assertThat(KoreaEximClient.isTlsFailure(new SSLHandshakeException("PKIX path building failed"))).isTrue();
    }

    @Test
    @DisplayName("IOException으로 감싸진 인증서 오류도 TLS 실패로 판단한다")
    void wrappedSslIsTlsFailure() {
        IOException wrapped = new IOException("request failed", new SSLHandshakeException("PKIX"));
        assertThat(KoreaEximClient.isTlsFailure(wrapped)).isTrue();
    }

    @Test
    @DisplayName("타임아웃과 연결 실패는 TLS 실패가 아니다 (재시도 대상)")
    void timeoutAndConnectAreNotTlsFailure() {
        assertThat(KoreaEximClient.isTlsFailure(new HttpTimeoutException("timed out"))).isFalse();
        assertThat(KoreaEximClient.isTlsFailure(new ConnectException("refused"))).isFalse();
    }
}