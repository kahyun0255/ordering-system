package com.orderingsystem.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final String refreshToken = "refresh-token";
    private final UUID userId = UUID.randomUUID();

    @DisplayName("리프레시 토큰 저장 및 조회에 성공한다.")
    @Test
    void saveRefreshTokenSuccessfully() {
        //given
        String beforeRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(beforeRefreshToken).isNull();

        //when
        refreshTokenRepository.save(userId, refreshToken, Duration.ofSeconds(10));

        //then
        String afterRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(afterRefreshToken).isEqualTo(refreshToken);
    }

    @DisplayName("리프레시 토큰 삭제에 성공한다.")
    @Test
    void deleteRefreshTokenSuccessfully() {
        //given
        refreshTokenRepository.save(userId, refreshToken, Duration.ofSeconds(10));
        String beforeRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(beforeRefreshToken).isEqualTo(refreshToken);

        //when
        refreshTokenRepository.delete(userId);

        //then
        String afterRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(afterRefreshToken).isNull();
    }

    @DisplayName("리프레시 토큰 업데이트에 성공한다.")
    @Test
    void updateRefreshTokenSuccessfully() {
        //given
        String updateRefreshToken = "update-refresh-token";

        refreshTokenRepository.save(userId, refreshToken, Duration.ofSeconds(10));
        String beforeRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(beforeRefreshToken).isEqualTo(refreshToken);

        //when
        refreshTokenRepository.update(userId, updateRefreshToken, Duration.ofSeconds(10));

        //then
        String afterRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(afterRefreshToken).isEqualTo(updateRefreshToken);
    }

}
