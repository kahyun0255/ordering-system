package com.orderingsystem.application.outbox;

import static com.orderingsystem.common.saga.SagaConstants.USER_CREATED_NAME;
import static com.orderingsystem.common.saga.SagaConstants.USER_DELETE_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.outbox.model.UserCreatedEventPayload;
import com.orderingsystem.application.outbox.model.UserDeletedEventPayload;
import com.orderingsystem.application.outbox.model.UserEventPayload;
import com.orderingsystem.domain.exception.UserDomainException;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.domain.model.outbox.UserOutbox;
import com.orderingsystem.domain.repository.outbox.UserOutboxRepository;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserOutboxHelper {

    private final UserOutboxRepository userOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(UserOutbox userOutbox) {
        if (!userOutboxRepository.existsByTypeAndEventId(USER_CREATED_NAME, userOutbox.getEventId())) {
            userOutboxRepository.save(userOutbox);
            log.info("User UserOutbox 저장. EventId : {}, Type : {}", userOutbox.getEventId(), userOutbox.getType());
        } else {
            log.warn("이미 저장된 User UserOutbox가 존재합니다. EventId : {}, Type : {}", userOutbox.getEventId(),
                    userOutbox.getType());
        }
    }

    @Transactional
    public void saveUserOutboxMessage(UserCreatedEventPayload userCreatedEventPayload, UUID eventId,
                                      UserType userType) {
        save(UserOutbox.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .createdAt(userCreatedEventPayload.getCreatedAt())
                .type(USER_CREATED_NAME)
                .userType(userType)
                .payload(createPayload(userCreatedEventPayload))
                .build());
    }

    @Transactional
    public void deleteUserOutboxMessage(UserDeletedEventPayload userDeletedEventPayload,
                                        UUID eventId, UserType userType) {
        save(UserOutbox.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .createdAt(ZonedDateTime.now())
                .type(USER_DELETE_NAME)
                .userType(userType)
                .payload(createPayload(userDeletedEventPayload))
                .build());
    }

    private String createPayload(UserEventPayload userEventPayload) {
        try {
            return objectMapper.writeValueAsString(userEventPayload);
        } catch (JsonProcessingException e) {
            log.error("CustomerEventPayload 생성에 실패했습니다. User Id : {}", userEventPayload.getId());
            throw new UserDomainException(
                    "CustomerEventPayload 생성에 실패했습니다. User Id : " + userEventPayload.getId());
        }
    }

    public int deleteOlderThan(ZonedDateTime threshold) {
        return userOutboxRepository.deleteOlderThan(threshold);
    }
}
