package com.orderingsystem.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.application.dto.request.CreditApplicationRequest;
import com.orderingsystem.payment.application.dto.response.BalanceResponse;
import com.orderingsystem.payment.domain.event.CreditEvent;
import com.orderingsystem.payment.domain.exception.PaymentDomainException;
import com.orderingsystem.payment.domain.exception.PaymentNotFoundException;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.service.CreditDepositService;
import com.orderingsystem.payment.domain.service.CreditWithdrawService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditEntryRepository creditEntryRepository;

    @Mock
    private CreditHistoryRepository creditHistoryRepository;

    @Mock
    private CreditDepositService creditDepositService;

    @Mock
    private CreditWithdrawService creditWithdrawService;

    @InjectMocks
    private CreditService creditService;

    @DisplayName("credit 검증에 문제가 없으면 입금 후 잔액을 반환한다.")
    @Test
    void shouldReturnBalanceAfterDeposit_whenCreditValidationPasses() {
        //given
        UUID userId = UUID.randomUUID();

        CreditApplicationRequest request = mock(CreditApplicationRequest.class);

        CreditEntry creditEntry = mock(CreditEntry.class);
        given(creditEntryRepository.findByCustomerId(eq(userId))).willReturn(Optional.of(creditEntry));

        given(request.getAmount()).willReturn(BigDecimal.valueOf(1000));

        List<CreditHistory> histories = List.of(
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.CREDIT)
                        .amount(new Money(new BigDecimal("1000")))
                        .build(),
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.DEBIT)
                        .amount(new Money(new BigDecimal("300")))
                        .build()
        );
        given(creditHistoryRepository.findByCustomerId(eq(userId))).willReturn(histories);

        CreditEvent creditEvent = mock(CreditEvent.class);
        given(creditDepositService.deposit(same(creditEntry), any(Money.class), anyList(), anyList()))
                .willReturn(creditEvent);

        CreditHistory creditHistory = mock(CreditHistory.class);
        given(creditEvent.getCreditEntry()).willReturn(creditEntry);
        given(creditEvent.getCreditHistory()).willReturn(creditHistory);
        given(creditEvent.getCreditEntry().getTotalCreditAmount()).willReturn(new Money(BigDecimal.valueOf(1000)));

        //when
        BalanceResponse response = creditService.deposit(userId, request);

        //then
        verify(creditDepositService).deposit(same(creditEntry), any(Money.class), anyList(), anyList());
        assertThat(response.getBalance().compareTo(BigDecimal.valueOf(1000))).isZero();
    }

    @DisplayName("credit 검증에 문제가 발생하면 입금에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCreditValidationFails() {
        //given
        UUID userId = UUID.randomUUID();
        CreditApplicationRequest request = mock(CreditApplicationRequest.class);
        given(request.getAmount()).willReturn(BigDecimal.valueOf(1000));

        CreditEntry entry = mock(CreditEntry.class);
        given(creditEntryRepository.findByCustomerId(userId)).willReturn(Optional.of(entry));
        given(creditHistoryRepository.findByCustomerId(userId)).willReturn(List.of());

        String msg = "Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다.";

        given(creditDepositService.deposit(
                same(entry), any(Money.class), anyList(), anyList()
        )).willAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> failures = inv.getArgument(3, List.class);
            failures.add(msg);
            return mock(CreditEvent.class);
        });

        //when, then
        assertThatThrownBy(() -> creditService.deposit(userId, request))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining(msg);

        verify(creditEntryRepository, never()).save(any(CreditEntry.class));
        verify(creditHistoryRepository, never()).save(any(CreditHistory.class));
    }

    @DisplayName("credit 검증에 문제가 없으면 출금 후 잔액을 반환한다.")
    @Test
    void shouldReturnBalanceAfterWithdrawal_whenCreditValidationPasses() {
        //given
        UUID userId = UUID.randomUUID();

        CreditApplicationRequest request = mock(CreditApplicationRequest.class);

        CreditEntry creditEntry = mock(CreditEntry.class);
        given(creditEntryRepository.findByCustomerId(eq(userId))).willReturn(Optional.of(creditEntry));

        given(request.getAmount()).willReturn(BigDecimal.valueOf(1000));

        List<CreditHistory> histories = List.of(
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.CREDIT)
                        .amount(new Money(new BigDecimal("1000")))
                        .build(),
                CreditHistory.builder()
                        .customerId(userId)
                        .type(TransactionType.DEBIT)
                        .amount(new Money(new BigDecimal("300")))
                        .build()
        );
        given(creditHistoryRepository.findByCustomerId(eq(userId))).willReturn(histories);

        CreditEvent creditEvent = mock(CreditEvent.class);
        given(creditWithdrawService.withdraw(same(creditEntry), any(Money.class), anyList(), anyList()))
                .willReturn(creditEvent);

        CreditHistory creditHistory = mock(CreditHistory.class);
        given(creditEvent.getCreditEntry()).willReturn(creditEntry);
        given(creditEvent.getCreditHistory()).willReturn(creditHistory);
        given(creditEvent.getCreditEntry().getTotalCreditAmount()).willReturn(new Money(BigDecimal.valueOf(1000)));

        //when
        BalanceResponse response = creditService.withdraw(userId, request);

        //then
        verify(creditWithdrawService).withdraw(same(creditEntry), any(Money.class), anyList(), anyList());
        assertThat(response.getBalance().compareTo(BigDecimal.valueOf(1000))).isZero();
    }

    @DisplayName("credit 검증에 문제가 발생하면 출금에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenWithdrawalFailsDueToInvalidCredit() {
        //given
        UUID userId = UUID.randomUUID();
        CreditApplicationRequest request = mock(CreditApplicationRequest.class);
        given(request.getAmount()).willReturn(BigDecimal.valueOf(1000));

        CreditEntry entry = mock(CreditEntry.class);
        given(creditEntryRepository.findByCustomerId(userId)).willReturn(Optional.of(entry));
        given(creditHistoryRepository.findByCustomerId(userId)).willReturn(List.of());

        String msg = "Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다.";

        given(creditWithdrawService.withdraw(
                same(entry), any(Money.class), anyList(), anyList()
        )).willAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> failures = inv.getArgument(3, List.class);
            failures.add(msg);
            return mock(CreditEvent.class);
        });

        //when, then
        assertThatThrownBy(() -> creditService.withdraw(userId, request))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining(msg);

        verify(creditEntryRepository, never()).save(any(CreditEntry.class));
        verify(creditHistoryRepository, never()).save(any(CreditHistory.class));
    }

    @DisplayName("계좌가 존재하면 잔액 조회에 성공한다.")
    @Test
    void shouldReturnBalance_whenAccountExists() {
        //given
        UUID userId = UUID.randomUUID();

        CreditEntry creditEntry = mock(CreditEntry.class);

        given(creditEntryRepository.findByCustomerId(userId)).willReturn(Optional.of(creditEntry));
        given(creditEntry.getTotalCreditAmount()).willReturn(new Money(BigDecimal.valueOf(10000)));

        //when
        BalanceResponse balance = creditService.getBalance(userId);

        //then
        assertThat(balance.getBalance().compareTo(BigDecimal.valueOf(10000))).isZero();
    }

    @DisplayName("계좌가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenAccountDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();

        given(creditEntryRepository.findByCustomerId(userId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(()->creditService.getBalance(userId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("계좌가 존재하지 않습니다.");
    }

}
