package com.orderingsystem.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TokenResponse {
    String accessToken;
    String refreshToken;
}
