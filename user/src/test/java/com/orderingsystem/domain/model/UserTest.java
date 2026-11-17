package com.orderingsystem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.exception.UserDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("User 도메인 단위 테스트")
class UserTest {

    @DisplayName("관리자 유형으로 유저를 생성하면 예외가 발생한다.")
    @Test
    void throwException_whenCreateUserWithAdminType() {
        //when, then
        assertThatThrownBy(() -> User.createUser("test", "테스트 유저", UserType.ADMIN, "ENCODED_PASSWORD",
                "test@test.com", "테스트유저", "010-1234-5678"))
                .isInstanceOf(UserDomainException.class)
                .hasMessage("관리자 계정은 회원가입으로 생성할 수 없습니다.");
    }

    @DisplayName("관리자가 아닌 유형으로 유저를 생성하면 ACTIVE 상태로 생성된다.")
    @ParameterizedTest
    @EnumSource(value = UserType.class, names = {"CUSTOMER", "RESTAURANT_OWNER"})
    void createUser_whenTypeIsNotAdmin(UserType type) {
        //when
        UserCreatedEvent userCreatedEvent = User.createUser("test", "테스트 유저", type, "ENCODED_PASSWORD",
                "test@test.com", "테스트유저", "010-1234-5678");

        //then
        User user = userCreatedEvent.getUser();
        assertThat(user.getUserId()).isNotNull();
        assertThat(user.getType()).isEqualTo(type);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

}
