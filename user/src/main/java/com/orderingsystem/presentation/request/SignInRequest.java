package com.orderingsystem.presentation.request;

import com.orderingsystem.application.dto.request.SignInApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignInRequest {

    @NotBlank(message = "아이디는 생략이 불가능합니다.")
    String id;

    @NotBlank(message = "비밀번호는 생략이 불가능합니다.")
    String password;

    public SignInApplicationRequest toSignInApplicationRequest(){
        return SignInApplicationRequest.builder()
                .id(this.id)
                .password(this.password)
                .build();
    }

}
