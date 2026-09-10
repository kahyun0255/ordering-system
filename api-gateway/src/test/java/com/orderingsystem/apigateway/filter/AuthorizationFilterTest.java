package com.orderingsystem.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    private static final String SIGN_IN_PATH = "/api/users/auth/sign-in";
    private static final String SIGN_UP_PATH = "/api/users/auth/sign-up";

    @Mock
    private CommonJwtUtil commonJwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private AuthorizationFilter authorizationFilter;

    @BeforeEach
    void setUp() {
        authorizationFilter = new AuthorizationFilter(commonJwtUtil);
        ReflectionTestUtils.setField(authorizationFilter, "signInPath", SIGN_IN_PATH);
        ReflectionTestUtils.setField(authorizationFilter, "signUpPath", SIGN_UP_PATH);
    }

    private ServerWebExchange exchangeFor(String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            builder = builder.header("Authorization", authHeader);
        }
        return MockServerWebExchange.from(builder.build());
    }

    @Nested
    @DisplayName("permit-all 경로 (로그인/회원가입)")
    class PermitAll {

        @Test
        @DisplayName("정확히 sign-in 경로면 인증 없이 통과한다")
        void shouldPassWithoutAuth_whenPathIsExactSignInPath() {
            //given
            ServerWebExchange exchange = exchangeFor(SIGN_IN_PATH, null);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            verify(chain, times(1)).filter(exchange);
            verify(commonJwtUtil, never()).getUserRoleFromToken(any());
        }

        @Test
        @DisplayName("정확히 sign-up 경로면 인증 없이 통과한다")
        void shouldPassWithoutAuth_whenPathIsExactSignUpPath() {
            //given
            ServerWebExchange exchange = exchangeFor(SIGN_UP_PATH, null);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            verify(chain, times(1)).filter(exchange);
        }

        @Test
        @DisplayName("[보안 회귀 테스트] sign-in과 같은 세그먼트를 포함하지만 실제로는 다른 엔드포인트면 permit-all로 처리되지 않고 인증을 요구해야 한다 (기존 contains() 버그 재현 케이스)")
        void shouldRequireAuth_whenPathContainsSignInAsSubstringButIsNotPermitAllPath() {
            //given
            ServerWebExchange exchange = exchangeFor("/api/products/auth/sign-in-recommendations", null);

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("인증 헤더 검증")
    class AuthHeader {

        @Test
        @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
        void shouldReturn401_whenAuthHeaderIsMissing() {
            //given
            ServerWebExchange exchange = exchangeFor("/api/orders", null);

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("Bearer로 시작하지 않는 토큰이면 401을 반환한다")
        void shouldReturn401_whenTokenDoesNotStartWithBearer() {
            //given
            ServerWebExchange exchange = exchangeFor("/api/orders", "Basic abcdef");

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("admin 경로 권한 체크")
    class AdminPath {

        @Test
        @DisplayName("admin 경로에 ADMIN 권한이면 통과한다")
        void shouldPass_whenUserHasAdminRoleOnAdminPath() {
            //given
            String authHeader = "Bearer valid-admin-token";
            ServerWebExchange exchange = exchangeFor("/api/restaurants/admin/menu", authHeader);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(commonJwtUtil.getUserRoleFromToken(authHeader)).thenReturn(UserType.ADMIN);
            when(commonJwtUtil.getUserIdFromToken(authHeader)).thenReturn(UUID.randomUUID());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            verify(chain, times(1)).filter(exchange);
        }

        @Test
        @DisplayName("admin 경로에 ADMIN이 아닌 권한이면 403을 반환한다")
        void shouldReturn403_whenUserIsNotAdminOnAdminPath() {
            //given
            String authHeader = "Bearer valid-customer-token";
            ServerWebExchange exchange = exchangeFor("/api/restaurants/admin/menu", authHeader);
            when(commonJwtUtil.getUserRoleFromToken(authHeader)).thenReturn(UserType.CUSTOMER);
            when(commonJwtUtil.getUserIdFromToken(authHeader)).thenReturn(UUID.randomUUID());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("[보안 회귀 테스트] '/admin/'을 부분 문자열로만 포함하고 실제 admin 세그먼트가 아니면 관리자 권한을 요구하지 않아야 한다 (기존 contains(\"/admin/\") 오탐 케이스)")
        void shouldPassWithoutAdminRole_whenPathContainsAdminAsSubstringButIsNotAdminSegment() {
            //given
            String authHeader = "Bearer valid-customer-token";
            ServerWebExchange exchange = exchangeFor("/api/products/admin-events/list", authHeader);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(commonJwtUtil.getUserRoleFromToken(authHeader)).thenReturn(UserType.CUSTOMER);
            when(commonJwtUtil.getUserIdFromToken(authHeader)).thenReturn(UUID.randomUUID());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            verify(chain, times(1)).filter(exchange);
        }

        @Test
        @DisplayName("일반 사용자 경로는 ADMIN 여부와 무관하게 통과한다")
        void shouldPass_whenAccessingGeneralPathRegardlessOfRole() {
            //given
            String authHeader = "Bearer valid-customer-token";
            ServerWebExchange exchange = exchangeFor("/api/orders/123", authHeader);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(commonJwtUtil.getUserRoleFromToken(authHeader)).thenReturn(UserType.CUSTOMER);
            when(commonJwtUtil.getUserIdFromToken(authHeader)).thenReturn(UUID.randomUUID());

            //when
            authorizationFilter.filter(exchange, chain).block();

            //then
            verify(chain, times(1)).filter(exchange);
        }
    }

}
