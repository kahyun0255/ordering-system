package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.domain.repository.RefreshTokenRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@TestInstance(Lifecycle.PER_METHOD)
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final String refreshToken = "refresh-token";
    private final UUID userId = UUID.randomUUID();

    @DisplayName("리프레시 토큰 저장 및 조회에 성공한다.")
    @Test
    void saveRefreshTokenSuccessfully() {
        //when
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        //then
        String result = refreshTokenService.findRefreshToken(userId);

        assertThat(result).isEqualTo(refreshToken);
    }

    @DisplayName("리프레시 토큰을 성공적으로 업데이트 한다.")
    @Test
    void updateRefreshTokenSuccessfully() {
        //given
        String updateRefreshToken = "update-refresh-token";

        refreshTokenService.saveRefreshToken(userId, refreshToken);
        String before = refreshTokenService.findRefreshToken(userId);

        //when
        refreshTokenService.updateToken(userId, updateRefreshToken);

        //then
        String after = refreshTokenService.findRefreshToken(userId);
        assertThat(before).isEqualTo(refreshToken);
        assertThat(after).isNotEqualTo(before);
        assertThat(after).isEqualTo(updateRefreshToken);
    }

    @DisplayName("TTL이 만료되면 리프레시 토큰이 삭제된다.")
    @Test
    void refreshTokenIsDeletedAfterTtlExpires() throws InterruptedException {
        //given
        refreshTokenRepository.save(userId, refreshToken, Duration.ofMillis(2));

        //when
        Thread.sleep(3);

        //then
        assertThat(refreshTokenService.findRefreshToken(userId)).isNull();
    }
}
