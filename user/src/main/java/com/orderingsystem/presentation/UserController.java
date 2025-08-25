package com.orderingsystem.presentation;

import com.orderingsystem.application.UserService;
import com.orderingsystem.application.dto.response.UserProfileResponse;
import com.orderingsystem.common.util.CommonJwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CommonJwtUtil commonJwtUtil;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("Authorization") String authorizationHeader){
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

}
