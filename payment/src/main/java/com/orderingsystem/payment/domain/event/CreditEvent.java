package com.orderingsystem.payment.domain.event;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreditEvent implements DomainEvent<CreditEntry> {

    private final CreditEntry creditEntry;
    private final CreditHistory creditHistory;
    private final ZonedDateTime createdAt;
    private final List<String> failureMessages;

}
