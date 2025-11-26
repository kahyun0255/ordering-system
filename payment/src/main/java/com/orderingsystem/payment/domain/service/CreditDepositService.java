package com.orderingsystem.payment.domain.service;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.domain.event.CreditEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.CreditInfo;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditDepositService {

    private final ValidateCreditHistoryService validateCreditHistoryService;

    public CreditEvent deposit(CreditEntry entry, Money money, List<CreditHistory> creditHistories,
                               List<String> failureMessages) {
        CreditInfo creditInfo = CreditInfo.builder()
                .id(entry.getId())
                .customerId(entry.getCustomerId())
                .totalCreditAmount(entry.getTotalCreditAmount())
                .build();

        validateCreditHistoryService.validateCreditHistory(creditInfo, creditHistories, failureMessages);
        entry.addCreditAmount(money);

        CreditHistory creditHistory = CreditHistory.builder()
                .orderId(null)
                .type(TransactionType.CREDIT)
                .id(UUID.randomUUID())
                .amount(money)
                .paidAt(ZonedDateTime.now())
                .build();

        return CreditEvent.builder()
                .creditEntry(entry)
                .creditHistory(creditHistory)
                .failureMessages(failureMessages)
                .createdAt(ZonedDateTime.now())
                .build();
    }

}
