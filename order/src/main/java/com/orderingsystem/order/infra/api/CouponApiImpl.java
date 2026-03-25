package com.orderingsystem.order.infra.api;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.orderingsystem.order.application.dto.request.ValidationCouponApplicationRequest;
import com.orderingsystem.order.application.dto.response.CouponValidationResponse;
import com.orderingsystem.order.application.port.out.CouponApi;
import com.orderingsystem.order.infra.api.dto.request.CouponValidationRequest;
import io.netty.handler.timeout.ReadTimeoutException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class CouponApiImpl implements CouponApi {

    private final WebClient webClient;

    @Value("${url.coupon-endpoint}")
    private String couponEndpoint;

    public CouponApiImpl(@Value("${url.coupon}") String serviceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    @Override
    public CouponValidationResponse validateCoupons(ValidationCouponApplicationRequest request, UUID sagaId) {
        try {
            return webClient.post()
                    .uri(couponEndpoint)
                    .bodyValue(toValidationCommand(request, sagaId))
                    .retrieve()
                    .bodyToMono(CouponValidationResponse.class)
                    .block(Duration.ofSeconds(3));
        } catch (WebClientResponseException e) {
            log.warn("쿠폰 검증 실패. 상태코드: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(e.getStatusCode(), "쿠폰 검증에 실패했습니다.");

        } catch (ReadTimeoutException e) {
            log.error("쿠폰 서비스 응답 지연 (Timeout). Saga Id : {}", sagaId);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "쿠폰 서비스 응답이 지연되고 있습니다.");

        } catch (Exception e) {
            log.error("쿠폰 서비스 통신 중 알 수 없는 오류. Saga Id : {}, error : {} ", sagaId, e.toString());
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "일시적인 오류로 쿠폰을 검증할 수 없습니다.");
        }
    }

    private CouponValidationRequest toValidationCommand(ValidationCouponApplicationRequest request, UUID sagaId) {
        return CouponValidationRequest.builder()
                .customerId(request.getCustomerId())
                .couponIds(request.getCouponIds())
                .totalOrderAmount(request.getTotalOrderAmount())
                .sagaId(sagaId)
                .build();
    }

}
