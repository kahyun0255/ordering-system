package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.repository.UserRepository;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserServiceHelper {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserCreatedEvent persistUser(SignUpApplicationRequest signUpApplicationRequest) {
        validSignUp(signUpApplicationRequest);
        User user = getUser(signUpApplicationRequest);
        userRepository.save(user);
        return new UserCreatedEvent(user, ZonedDateTime.now());
    }

    private void validSignUp(SignUpApplicationRequest signUpApplicationRequest) {
        boolean existsById = userRepository.existsById(signUpApplicationRequest.getId());
        if (existsById){
            throw new DuplicateKeyException("이미 존재하는 아이디입니다.");
        }

        boolean existsByNickname = userRepository.existsByNickname(signUpApplicationRequest.getNickname());
        if (existsByNickname){
            throw new DuplicateKeyException("이미 존재하는 닉네임입니다.");
        }

        boolean existsByEmail = userRepository.existsByEmail(signUpApplicationRequest.getEmail());
        if (existsByEmail){
            throw new DuplicateKeyException("이미 존재하는 이메일입니다.");
        }
    }

    private User getUser(SignUpApplicationRequest signUpApplicationRequest) {
        return User.builder()
                .userId(UUID.randomUUID())
                .id(signUpApplicationRequest.getId())
                .username(signUpApplicationRequest.getUsername())
                .type(signUpApplicationRequest.getType())
                .password(passwordEncoder.encode(signUpApplicationRequest.getPassword()))
                .email(signUpApplicationRequest.getEmail())
                .nickname(signUpApplicationRequest.getNickname())
                .phoneNumber(signUpApplicationRequest.getPhoneNumber())
                .build();
    }

}
