package com.orderingsystem.presentation;

import com.orderingsystem.application.SignUpService;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.presentation.request.SignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final SignUpService signUpService;

    @PostMapping("/sign-up")
    public ResponseEntity<TokenResponse> signUp(@RequestBody SignUpRequest signUpRequest, BindingResult bindingResult){
        if (bindingResult.hasErrors()){
            String message = bindingResult.getFieldErrors().stream()
                    .map(e->e.getField()+": "+e.getDefaultMessage())
                    .reduce((a,b)->a+", "+b)
                    .orElse("잘못된 요청입니다.");
            throw new IllegalArgumentException(message);
        }

        return ResponseEntity.ok(signUpService.signUp(signUpRequest.toSignUpApplicationRequest()));
    }
}
