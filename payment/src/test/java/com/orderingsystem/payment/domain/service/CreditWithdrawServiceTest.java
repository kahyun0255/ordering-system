package com.orderingsystem.payment.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.domain.event.CreditEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditWithdrawServiceTest {

    @Mock
    private ValidateCreditHistoryService validateCreditHistoryService;

    @InjectMocks
    private CreditWithdrawService creditWithdrawService;

    @DisplayName("출금할 잔액이 충분하면 CreditEvent를 반환한다.")
    @Test
    void shouldReturnCreditEvent_whenWithdrawalAmountIsSufficient() {
        //given
        UUID userId = UUID.randomUUID();

        CreditEntry entry = CreditEntry.builder()
                .id(userId)
                .customerId(UUID.randomUUID())
                .totalCreditAmount(new Money(BigDecimal.valueOf(999700)))
                .build();

        Money money = new Money(BigDecimal.valueOf(100));

        List<CreditHistory> histories = List.of(
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.CREDIT)
                        .amount(new Money(new BigDecimal("1000000")))
                        .build(),
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.DEBIT)
                        .amount(new Money(new BigDecimal("300")))
                        .build()
        );

        List<String> failureMessage = new ArrayList<>();

        //when
        CreditEvent creditEvent = creditWithdrawService.withdraw(entry, money, histories, failureMessage);

        //then
        assertThat(creditEvent.getCreditEntry().getTotalCreditAmount().getAmount()
                .compareTo(BigDecimal.valueOf(999600))).isZero();

        assertThat(creditEvent.getCreditHistory().getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(creditEvent.getCreditHistory().getAmount().getAmount().compareTo(BigDecimal.valueOf(100))).isZero();
    }

    @DisplayName("출금할 잔액이 부족하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenWithdrawalAmountIsInsufficient() {
        //given
        UUID userId = UUID.randomUUID();

        CreditEntry entry = CreditEntry.builder()
                .id(userId)
                .customerId(UUID.randomUUID())
                .totalCreditAmount(new Money(BigDecimal.valueOf(100)))
                .build();

        Money money = new Money(BigDecimal.valueOf(1000));

        List<CreditHistory> histories = List.of(
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.CREDIT)
                        .amount(new Money(new BigDecimal("100")))
                        .build()
        );

        List<String> failureMessage = new ArrayList<>();

        //when, then
        assertThatThrownBy(()->creditWithdrawService.withdraw(entry, money, histories, failureMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
    }

}
