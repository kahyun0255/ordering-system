package com.orderingsystem.apigateway.filter;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {

    private final CommonJwtUtil commonJwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        String authHeader = request.getHeaders().getFirst("Authorization");
        UserType role = commonJwtUtil.getUserRoleFromToken(authHeader);
        UUID userId = commonJwtUtil.getUserIdFromToken(authHeader);

        if (path.contains("/admin/")){
            if (!UserType.ADMIN.equals(role)){
                log.info("관리자가 아닌 유저가 관리자 경로 접근 시도. User ID : [{}], Path : [{}]", userId, path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        log.info("요청이 gateway를 통과했습니다. User Id : [{}], Path : [{}]", userId, path);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

}
