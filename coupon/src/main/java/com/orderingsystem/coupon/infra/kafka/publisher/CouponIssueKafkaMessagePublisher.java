package com.orderingsystem.coupon.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderingsystem.coupon.application.port.out.CouponIssueMessagePublisher;
import com.orderingsystem.coupon.domain.event.CouponIssuedEvent;
import com.orderingsystem.coupon.infra.kafka.message.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponIssueKafkaMessagePublisher implements CouponIssueMessagePublisher {

    private final KafkaTemplate<String, String> kafkaProducer;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${coupon-service.coupon-issue-topic-name}")
    public String topicName;

    @Override
    public void publish(CouponIssuedEvent event) {
        String key = event.getUserId().toString();
        CouponIssueMessage couponIssueMessage = buildMessage(event);

        log.info("CouponIssueKafkaMessagePublisher request 전송 시작. topic : {}, key : {}", topicName, key);

        try {
            String message = objectMapper.writeValueAsString(couponIssueMessage);
            kafkaProducer.send(topicName, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("CouponIssueKafkaMessagePublisher Kafka Message 전송 성공. Offset : {}",
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("CouponIssueKafkaMessagePublisher Kafka Message 전송 실패.", ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("CouponIssueKafkaMessagePublisher json 변환 에러", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("CouponIssueKafkaMessagePublisher Error publishing Message", e);
            throw new RuntimeException("Kafka publish 실패", e);
        }
    }

    private CouponIssueMessage buildMessage(CouponIssuedEvent event) {
        return CouponIssueMessage.builder()
                .couponId(event.getCouponId())
                .userId(event.getUserId())
                .issuedAt(event.getCreatedAt())
                .build();
    }

}
