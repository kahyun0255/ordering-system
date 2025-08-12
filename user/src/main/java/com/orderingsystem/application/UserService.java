package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.mapper.UserDataMapper;
import com.orderingsystem.application.outbox.UserOutboxHelper;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.exception.UserNotFoundException;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserOutboxHelper userOutboxHelper;
    private final UserDataMapper userDataMapper;

    @Transactional
    public UserCreatedEvent persistUser(SignUpApplicationRequest signUpApplicationRequest) {
        validSignUp(signUpApplicationRequest);
        User user = getUser(signUpApplicationRequest);
        userRepository.save(user);

        userOutboxHelper.saveUserOutboxMessage(
                userDataMapper.userCreatedToUserCreateEventPayload(user),
                OutboxStatus.STARTED,
                UUID.randomUUID(),
                signUpApplicationRequest.getType()
        );

        return new UserCreatedEvent(user, ZonedDateTime.now());
    }

    public void verifyPassword(String rowPassword, String userPassword) {
        if (!passwordEncoder.matches(rowPassword, userPassword)) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }
    }

    @Transactional
    public User findUserByUserId(UUID userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자입니다. User Id : {}", userId);
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return user.get();
    }

    @Transactional
    public User findUser(String id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자입니다. Id : {}", id);
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return user.get();
    }

    private void validSignUp(SignUpApplicationRequest signUpApplicationRequest) {
        boolean existsById = userRepository.existsById(signUpApplicationRequest.getId());
        if (existsById) {
            throw new DuplicateKeyException("이미 존재하는 아이디입니다.");
        }

        boolean existsByNickname = userRepository.existsByNickname(signUpApplicationRequest.getNickname());
        if (existsByNickname) {
            throw new DuplicateKeyException("이미 존재하는 닉네임입니다.");
        }

        boolean existsByEmail = userRepository.existsByEmail(signUpApplicationRequest.getEmail());
        if (existsByEmail) {
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
