package com.orderingsystem.payment.domain.service;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.CreditInfo;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidateCreditHistoryService {

    public void validateCreditHistory(CreditInfo creditInfo, List<CreditHistory> creditHistories,
                                       List<String> failureMessages) {
        Money totalCreditHistory = getTotalHistoryAmount(creditHistories, TransactionType.CREDIT);
        Money totalDebitHistory = getTotalHistoryAmount(creditHistories, TransactionType.DEBIT);

        if (totalDebitHistory.isGreaterThan(totalCreditHistory)) {
            log.warn("Credit History 기준으로 크레딧이 부족합니다.(총 사용액이 총 충전액보다 큽니다.). Customer Id : {}",
                    creditInfo.getCustomerId());
            failureMessages.add("Credit History 기준으로 크레딧이 부족합니다.");
        }

        if (!creditInfo.getTotalCreditAmount().equals(totalCreditHistory.subtract(totalDebitHistory))) {
            log.warn("Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다. Customer Id : {}", creditInfo.getCustomerId());
            failureMessages.add("Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다.");
        }
    }

    public Money getTotalHistoryAmount(List<CreditHistory> creditHistories, TransactionType transactionType) {
        return creditHistories.stream()
                .filter(creditHistory -> transactionType.equals(creditHistory.getType()))
                .map(CreditHistory::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

}
