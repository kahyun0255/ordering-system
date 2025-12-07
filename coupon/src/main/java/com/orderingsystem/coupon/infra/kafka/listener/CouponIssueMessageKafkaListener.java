package com.orderingsystem.coupon.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderingsystem.coupon.application.IssueCouponService;
import com.orderingsystem.coupon.application.dto.request.CouponIssueApplicationRequest;
import com.orderingsystem.coupon.infra.kafka.message.CouponIssueMessage;
import com.orderingsystem.kafka.KafkaConsumer;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponIssueMessageKafkaListener implements KafkaConsumer<String> {

    private final IssueCouponService issueCouponService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    @KafkaListener(id = "${kafka-consumer-config.coupon-group-id}",
            topics = "${coupon-service.coupon-issue-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("쿠폰 발급 요청 {}건 수신. keys:{}, partitions:{}, offsets:{}",
                messages.size(), keys, partitions, offsets);

        List<CouponIssueApplicationRequest> requests = new ArrayList<>();

        for (String json : messages) {
            try {
                CouponIssueMessage message = objectMapper.readValue(json, CouponIssueMessage.class);
                requests.add(message.toCouponIssueApplicationRequest());
            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패. data: {}", json, e);
            }
        }

        if (!requests.isEmpty()) {
            issueCouponService.saveIssuedCoupon(requests);
            log.info("쿠폰 발급 배치 처리 완료: {}건", requests.size());
        }
    }

}
