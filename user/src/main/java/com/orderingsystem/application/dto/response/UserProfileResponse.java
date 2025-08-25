package com.orderingsystem.application.dto.response;

import com.orderingsystem.domain.model.UserType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserProfileResponse {

    private final String id;
    private final String username;
    private final String email;
    private final String nickname;
    private final String phoneNumber;
    private final UserType type;

}
