package com.orderingsystem.presentation;

import com.orderingsystem.application.AuthFacade;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.presentation.request.SignInRequest;
import com.orderingsystem.presentation.request.SignUpRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthFacade authFacade;

    @PostMapping("/sign-up")
    public ResponseEntity<TokenResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest,
                                                BindingResult bindingResult) {
        valid(bindingResult);
        return ResponseEntity.ok(authFacade.signUp(signUpRequest.toSignUpApplicationRequest()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<TokenResponse> signIn(@Valid @RequestBody SignInRequest signInRequest,
                                                BindingResult bindingResult, HttpServletResponse httpServletResponse) {
        valid(bindingResult);
        TokenResponse tokenResponse = authFacade.signIn(signInRequest.toSignInApplicationRequest());

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(14))
                .build();
        httpServletResponse.addHeader("Set-Cookie", responseCookie.toString());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {

        TokenResponse rotated = authFacade.rotateRefreshAndIssueAccess(refreshToken);

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", rotated.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(14))
                .build();
        httpServletResponse.addHeader("Set-Cookie", responseCookie.toString());

        return ResponseEntity.ok(rotated);
    }

    private static void valid(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("잘못된 요청입니다.");
            throw new IllegalArgumentException(message);
        }
    }

}
