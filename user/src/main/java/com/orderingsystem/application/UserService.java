package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.request.UpdateUserApplicationRequest;
import com.orderingsystem.application.dto.response.UserProfileResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
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

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserByUserId(userId);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .type(user.getType())
                .build();
    }

    @Transactional
    public UserProfileResponse updateUser(UUID userId, UpdateUserApplicationRequest updateUserApplicationRequest) {
        User user = findUserByUserId(userId);
        log.info("유저 정보 업데이트. User Id : {}, nicknameBefore : {}, nicknameAfter : {}",
                userId, user.getNickname(), updateUserApplicationRequest.getNickname());

        if (updateUserApplicationRequest.getNickname() != null &&
                !user.getNickname().equals(updateUserApplicationRequest.getNickname())) {
            user.updateNickname(updateUserApplicationRequest.getNickname());
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .type(user.getType())
                .build();
    }

    public void verifyPassword(String rowPassword, String userPassword) {
        if (!passwordEncoder.matches(rowPassword, userPassword)) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public User findUserByUserId(UUID userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자입니다. User Id : {}", userId);
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return user.get();
    }

    @Transactional(readOnly = true)
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
