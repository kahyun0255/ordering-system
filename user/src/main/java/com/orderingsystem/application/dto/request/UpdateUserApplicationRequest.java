package com.orderingsystem.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateUserApplicationRequest {

    private final String nickname;

}
