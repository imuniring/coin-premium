package com.uni.coinpremium.collector.adapter.out.koreaexim;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.http.HttpTimeoutException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KoreaEximClientTest {

    @Test
    @DisplayName("인증서 검증 실패(PKIX)는 인증서 문제로 판단한다")
    void certificateFailure() {
        SSLHandshakeException pkix = handshake("PKIX path building failed", new CertificateException("untrusted"));
        assertThat(KoreaEximClient.isCertificateFailure(pkix)).isTrue();
    }

    @Test
    @DisplayName("IOException으로 감싸진 인증서 검증 실패도 인증서 문제로 판단한다")
    void wrappedCertificateFailure() {
        IOException wrapped = new IOException("request failed",
                handshake("PKIX", new CertificateException("untrusted")));
        assertThat(KoreaEximClient.isCertificateFailure(wrapped)).isTrue();
    }

    @Test
    @DisplayName("핸드셰이크 중 연결이 끊긴 경우는 인증서 문제가 아니다 (재시도 대상)")
    void handshakeResetIsNotCertificateFailure() {
        // 실제 발생: Remote host terminated the handshake ← Connection reset
        SSLHandshakeException reset = handshake("Remote host terminated the handshake",
                new SocketException("Connection reset"));
        assertThat(KoreaEximClient.isCertificateFailure(reset)).isFalse();
    }

    @Test
    @DisplayName("타임아웃과 연결 실패는 인증서 문제가 아니다 (재시도 대상)")
    void timeoutAndConnect() {
        assertThat(KoreaEximClient.isCertificateFailure(new HttpTimeoutException("timed out"))).isFalse();
        assertThat(KoreaEximClient.isCertificateFailure(new ConnectException("refused"))).isFalse();
    }

    // SSLHandshakeException은 원인을 받는 생성자가 없어 initCause로 연결
    private static SSLHandshakeException handshake(String message, Throwable cause) {
        SSLHandshakeException e = new SSLHandshakeException(message);
        e.initCause(cause);
        return e;
    }
}