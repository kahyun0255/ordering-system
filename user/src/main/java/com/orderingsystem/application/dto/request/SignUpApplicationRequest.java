package com.orderingsystem.application.dto.request;

import com.orderingsystem.domain.model.UserType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpApplicationRequest {

    private String username;
    private String id;
    private String password;
    private String email;
    private String nickname;
    private String phoneNumber;
    private UserType type;

}
