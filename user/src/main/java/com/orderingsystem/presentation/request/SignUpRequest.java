package com.orderingsystem.presentation.request;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.common.domain.status.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpRequest {

    @NotBlank(message = "이름은 생략이 불가능합니다.")
    private String username;

    @NotBlank(message = "아이디는 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "아이디는 2 ~ 30자여야 합니다.")
    private String id;

    @NotBlank(message = "비밀번호는 생략이 불가능합니다.")
    @Size(min = 8, max=64, message = "비밀번호는 8 ~ 64자여야 합니다.")
    private String password;

    @NotBlank(message = "이메일은 생략이 불가능합니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "닉네임은 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "닉네임은 2 ~ 30자여야 합니다.")
    private String nickname;

    @Pattern(regexp = "^$|^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "유효한 휴대폰 번호 형식이 아닙니다.")
    private String phoneNumber;

    private UserType type;

    public SignUpApplicationRequest toSignUpApplicationRequest(){
        return SignUpApplicationRequest.builder()
                .username(this.username)
                .id(this.id)
                .password(this.password)
                .email(this.email)
                .nickname(this.nickname)
                .phoneNumber(this.phoneNumber)
                .type(this.type)
                .build();
    }

}
