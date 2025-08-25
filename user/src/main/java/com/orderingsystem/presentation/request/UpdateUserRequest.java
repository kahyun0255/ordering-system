package com.orderingsystem.presentation.request;

import com.orderingsystem.application.dto.request.UpdateUserApplicationRequest;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateUserRequest {

    @Size(min = 2, max = 30, message = "닉네임은 2 ~ 30자여야 합니다.")
    private final String nickname;

    public UpdateUserApplicationRequest toUpdateUserApplicationRequest(){
        return UpdateUserApplicationRequest.builder()
                .nickname(this.nickname)
                .build();
    }

}
