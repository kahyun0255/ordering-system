package com.orderingsystem.apigateway.filter;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {

    private static final String ADMIN_PATH_PATTERN = "/api/*/admin/**";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final CommonJwtUtil commonJwtUtil;

    @Value("${path.sign-in}")
    private String signInPath;

    @Value("${path.sign-up}")
    private String signUpPath;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPermitAllPath(path)) {
            log.info("로그인/회원가입 요청  Path : [{}]", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            log.info("인증 토큰이 없습니다. Path : [{}]", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        UserType role = commonJwtUtil.getUserRoleFromToken(authHeader);
        UUID userId = commonJwtUtil.getUserIdFromToken(authHeader);

        if (isAdminPath(path) && !UserType.ADMIN.equals(role)) {
            log.info("관리자가 아닌 유저가 관리자 경로 접근 시도. User ID : [{}], Path : [{}]", userId, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        log.info("요청이 gateway를 통과했습니다. User Id : [{}], Path : [{}]", userId, path);
        return chain.filter(exchange);
    }

    private boolean isPermitAllPath(String path) {
        return PATH_MATCHER.match(signInPath, path) || PATH_MATCHER.match(signUpPath, path);
    }

    private boolean isAdminPath(String path) {
        return PATH_MATCHER.match(ADMIN_PATH_PATTERN, path);
    }

    @Override
    public int getOrder() {
        return -1;
    }

}
