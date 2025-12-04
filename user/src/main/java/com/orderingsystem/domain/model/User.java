package com.orderingsystem.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.event.UserDeletedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends AggregateRoot {

    @Id
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType type;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public UserDeletedEvent withdraw() {
        this.status = UserStatus.WITHDRAWN;
        return new UserDeletedEvent(this, ZonedDateTime.now());
    }

    public static UserCreatedEvent createUser(String id, String username, UserType type, String password, String email, String nickname, String phoneNumber){
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id(id)
                .username(username)
                .type(type)
                .password(password)
                .email(email)
                .nickname(nickname)
                .phoneNumber(phoneNumber)
                .status(UserStatus.ACTIVE)
                .build();

        return new UserCreatedEvent(user, ZonedDateTime.now());
    }
}
