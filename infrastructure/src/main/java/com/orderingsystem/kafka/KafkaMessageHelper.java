package com.orderingsystem.kafka;

import com.orderingsystem.outbox.OutboxStatus;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class KafkaMessageHelper {

    public <T, U> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(
            String topicName, T message, U outboxMessage, BiConsumer<U, OutboxStatus> outboxCallBack, String orderId) {

        return new BiConsumer<SendResult<String, T>, Throwable>() {

            @Override
            public void accept(SendResult<String, T> stringTSendResult, Throwable throwable) {
                if (throwable != null) {
                    log.error("Kafka 메시지 전송 중 오류가 발생했습니다. Message : {}, Outbox Type : {}, Topic : {}",
                            message, outboxMessage.getClass().getName(), topicName, throwable);
                    outboxCallBack.accept(outboxMessage, OutboxStatus.FAILED);
                } else {
                    ProducerRecord<String, T> record = stringTSendResult.getProducerRecord();
                    log.info("Kafka 전송에 성공했습니다. Order Id : {}, Topic : {}, Partition : {}, Offset : {}, Timestamp : {}",
                            orderId,
                            record.topic(),
                            record.partition(),
                            stringTSendResult.getRecordMetadata().offset(),
                            record.timestamp()!=null?record.timestamp() : "N/A");
                    outboxCallBack.accept(outboxMessage, OutboxStatus.COMPLETED);
                }
            }
        };
    }

}
