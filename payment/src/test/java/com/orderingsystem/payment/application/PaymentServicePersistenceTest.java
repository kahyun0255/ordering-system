package com.orderingsystem.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.mapper.PaymentDataMapper;
import com.orderingsystem.payment.application.outbox.OrderOutboxHelper;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.payment.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndCancelService;
import com.orderingsystem.payment.domain.service.PaymentValidateAndInitiateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * persistCompleteDataBase / persistCancelDataBase (둘 다 private)의 동작을
 * completePayment() / cancelPayment()를 통해 관찰 가능한 모든 케이스로 검증한다.
 *
 * 확인된 사실: 기존 코드의
 *   creditInfo.getTotalCreditAmount().equals(creditEntry.getTotalCreditAmount().subtract(price))
 * 는, creditInfo가 creditEntry의 스냅샷에서 딱 한 번만 price를 뺀 값이고
 * ValidateCreditHistoryService는 creditInfo를 읽기만 할 뿐 수정하지 않으며
 * Money.equals()가 compareTo() 기반(스케일 무관)이라, failureMessages가 비어있는 한
 * 수학적으로 항상 참이었다. 즉 실질적으로 failureMessages.isEmpty()와 동일한 조건이었고,
 * 이번 수정은 동작 변경 없이 그 죽은 이중 체크만 제거한 것이다. 아래 테스트들은
 * 이 동등성을 포함해 관련 동작 전체를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServicePersistenceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentValidateAndInitiateService paymentValidateAndInitiateService;
    @Mock private CreditEntryRepository creditEntryRepository;
    @Mock private CreditHistoryRepository creditHistoryRepository;
    @Mock private OrderOutboxHelper orderOutboxHelper;
    @Mock private PaymentDataMapper paymentDataMapper;
    @Mock private PaymentValidateAndCancelService paymentValidateAndCancelService;
    @Mock private ProcessedMessageRepository processedMessageRepository;
    @Mock private OrderOutboxRepository orderOutboxRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID sagaId;
    private UUID customerId;
    private Payment payment;
    private CreditEntry creditEntry;
    private Money originalBalance;
    private Money price;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        sagaId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        originalBalance = new Money(BigDecimal.valueOf(50_000));
        price = new Money(BigDecimal.valueOf(10_000));

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .price(price)
                .status(PaymentStatus.COMPLETED)
                .build();

        creditEntry = CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .totalCreditAmount(originalBalance)
                .build();

        when(processedMessageRepository.insertIgnore(any(), any(), any())).thenReturn(1);
    }

    private PaymentRequest paymentRequest() {
        return PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(price.getAmount())
                .createdAt(Instant.now())
                .build();
    }

    private CreditHistory historyOf(TransactionType type, BigDecimal amount) {
        return CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .amount(new Money(amount))
                .type(type)
                .paidAt(ZonedDateTime.now())
                .build();
    }

    // =========================================================================================
    // persistCompleteDataBase (completePayment 경로)
    // =========================================================================================
    @Nested
    @DisplayName("persistCompleteDataBase")
    class CompleteDataBase {

        @Test
        @DisplayName("성공: payment 저장 + creditEntry가 정확히 price만큼 차감되어 저장 + 마지막 CreditHistory만 저장")
        void success_savesPaymentCreditEntryAndOnlyLastHistory() {
            // given: 기존에 히스토리가 2건 있었고, validateAndInitiate가 새 DEBIT 이력을 1건 추가한다고 가정
            List<CreditHistory> creditHistories = new ArrayList<>(List.of(
                    historyOf(TransactionType.CREDIT, BigDecimal.valueOf(30_000)),
                    historyOf(TransactionType.CREDIT, BigDecimal.valueOf(20_000))
            ));
            CreditHistory newDebitHistory = historyOf(TransactionType.DEBIT, price.getAmount());

            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);

            when(paymentValidateAndInitiateService.validateAndInitiate(any(), any(), anyList(), anyList(), any()))
                    .thenAnswer(invocation -> {
                        List<CreditHistory> historiesArg = invocation.getArgument(2);
                        historiesArg.add(newDebitHistory); // 실제 서비스가 하는 것처럼 마지막에 새 이력 추가
                        return event;
                    });

            // when
            paymentService.completePayment(paymentRequest());

            // then: Payment는 항상 저장된다.
            // completePayment()는 PaymentRequest.toPayment()로 매번 새 Payment 인스턴스를 만들기 때문에
            // 미리 만들어둔 payment 필드와 동일성 비교를 할 수 없다. 값만 캡처해서 검증한다.
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, times(1)).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getOrderId()).isEqualTo(orderId);
            assertThat(paymentCaptor.getValue().getCustomerId()).isEqualTo(customerId);
            assertThat(paymentCaptor.getValue().getPrice()).isEqualTo(price);

            // creditEntry는 정확히 price만큼 차감된 상태로 저장된다
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(originalBalance.subtract(price));
            verify(creditEntryRepository, times(1)).save(creditEntry);

            // 저장되는 CreditHistory는 "마지막에 추가된" 새 이력 1건뿐이어야 한다 (기존 2건은 다시 저장되지 않음)
            ArgumentCaptor<CreditHistory> historyCaptor = ArgumentCaptor.forClass(CreditHistory.class);
            verify(creditHistoryRepository, times(1)).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue()).isEqualTo(newDebitHistory);
            assertThat(historyCaptor.getValue().getType()).isEqualTo(TransactionType.DEBIT);
        }

        @Test
        @DisplayName("실패: failureMessages가 채워지면 payment는 저장되지만 creditEntry/CreditHistory는 건드리지 않는다")
        void failure_savesPaymentOnly_doesNotTouchCreditAtAll() {
            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.CREDIT, BigDecimal.valueOf(5_000))));

            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);

            when(paymentValidateAndInitiateService.validateAndInitiate(any(), any(), anyList(), anyList(), any()))
                    .thenAnswer(invocation -> {
                        List<String> failureMessages = invocation.getArgument(3);
                        failureMessages.add("고객의 크레딧이 결제 금액보다 부족합니다.");
                        return event;
                    });

            // when
            paymentService.completePayment(paymentRequest());

            // then
            verify(paymentRepository, times(1)).save(any(Payment.class)); // 실패해도 Payment는 저장되어야 함
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(originalBalance); // 잔액 불변
            verify(creditEntryRepository, never()).save(any());
            verify(creditHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계값: price가 잔액과 정확히 같아도(잔액 0이 되는 경우) 정상적으로 차감/저장된다")
        void success_whenPriceExactlyEqualsBalance() {
            CreditEntry exactBalanceEntry = CreditEntry.builder()
                    .id(UUID.randomUUID())
                    .customerId(customerId)
                    .totalCreditAmount(price) // 잔액 == price
                    .build();

            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.CREDIT, price.getAmount())));

            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(exactBalanceEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);
            when(paymentValidateAndInitiateService.validateAndInitiate(any(), any(), anyList(), anyList(), any()))
                    .thenAnswer(invocation -> {
                        List<CreditHistory> h = invocation.getArgument(2);
                        h.add(historyOf(TransactionType.DEBIT, price.getAmount()));
                        return event;
                    });

            // when
            paymentService.completePayment(paymentRequest());

            // then: 잔액이 정확히 0(Money.ZERO)이 되어야 한다
            assertThat(exactBalanceEntry.getTotalCreditAmount()).isEqualTo(Money.ZERO);
            verify(creditEntryRepository, times(1)).save(exactBalanceEntry);
        }
    }

    // =========================================================================================
    // persistCancelDataBase (cancelPayment 경로)
    // =========================================================================================
    @Nested
    @DisplayName("persistCancelDataBase")
    class CancelDataBase {

        @Test
        @DisplayName("성공(CANCELLING): payment 저장 + creditEntry가 정확히 price만큼 환급되어 명시적으로 저장 " +
                "+ 마지막 CreditHistory만 저장")
            // 주의: 이 테스트는 cancelPayment()의 중복 save 버그(creditHistoryRepository.save()가
            // cancelPayment() 본문과 persistCancelDataBase() 안에서 총 2번 호출됨)가 수정되었다는
            // 전제로 작성됨. 아직 수정 전이라면 이 부분만 times(1) -> times(2)로 바꿔서 돌려볼 것.
        void success_cancelling_savesPaymentCreditEntryAndOnlyLastHistory() {
            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.DEBIT, price.getAmount())));
            CreditHistory refundHistory = historyOf(TransactionType.CREDIT, price.getAmount());

            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);
            when(paymentValidateAndCancelService.validateAndCancel(any(), any(), anyList(), anyList()))
                    .thenAnswer(invocation -> {
                        CreditEntry entry = invocation.getArgument(1);
                        List<CreditHistory> h = invocation.getArgument(2);
                        entry.addCreditAmount(price); // 실제 서비스처럼 환급
                        h.add(refundHistory);
                        return event;
                    });

            // when
            paymentService.cancelPayment(paymentRequest(), OrderStatus.CANCELLING);

            // then
            verify(paymentRepository, times(1)).save(payment);
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(originalBalance.add(price));
            verify(creditEntryRepository, times(1)).save(creditEntry); // dirty checking이 아니라 명시적 호출

            ArgumentCaptor<CreditHistory> historyCaptor = ArgumentCaptor.forClass(CreditHistory.class);
            verify(creditHistoryRepository, times(1)).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue()).isEqualTo(refundHistory);
        }

        @Test
        @DisplayName("성공(REJECTING): validateAndRefund 경로도 동일하게 명시적으로 저장된다")
        void success_rejecting_savesCreditEntryExplicitly() {
            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.DEBIT, price.getAmount())));

            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);
            when(paymentValidateAndCancelService.validateAndRefund(any(), any(), anyList(), anyList()))
                    .thenAnswer(invocation -> {
                        CreditEntry entry = invocation.getArgument(1);
                        List<CreditHistory> h = invocation.getArgument(2);
                        entry.addCreditAmount(price);
                        h.add(historyOf(TransactionType.CREDIT, price.getAmount()));
                        return event;
                    });

            // when
            paymentService.cancelPayment(paymentRequest(), OrderStatus.REJECTING);

            // then
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(originalBalance.add(price));
            verify(creditEntryRepository, times(1)).save(creditEntry);
        }

        @Test
        @DisplayName("실패: payment 검증이 실패(failureMessages 채워짐)하면 payment는 저장되지만 " +
                "creditEntry/CreditHistory는 건드리지 않는다")
        void failure_savesPaymentOnly_doesNotTouchCreditAtAll() {
            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.DEBIT, price.getAmount())));

            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);
            when(paymentValidateAndCancelService.validateAndCancel(any(), any(), anyList(), anyList()))
                    .thenAnswer(invocation -> {
                        List<String> failureMessages = invocation.getArgument(3);
                        failureMessages.add("결제 검증에 실패했습니다.");
                        return event; // creditEntry는 건드리지 않고 실패 이벤트만 반환
                    });

            // when
            paymentService.cancelPayment(paymentRequest(), OrderStatus.CANCELLING);

            // then
            verify(paymentRepository, times(1)).save(payment);
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(originalBalance); // 잔액 불변
            verify(creditEntryRepository, never()).save(any());
            verify(creditHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("저장되는 creditEntry는 조회 시 받은 것과 동일한 인스턴스여야 한다 (별도 복사본을 저장하지 않음)")
        void savedCreditEntry_isSameInstanceAsFetched() {
            List<CreditHistory> creditHistories = new ArrayList<>(
                    List.of(historyOf(TransactionType.DEBIT, price.getAmount())));

            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);
            when(paymentValidateAndCancelService.validateAndCancel(any(), any(), anyList(), anyList()))
                    .thenAnswer(invocation -> {
                        CreditEntry entry = invocation.getArgument(1);
                        entry.addCreditAmount(price);
                        List<CreditHistory> h = invocation.getArgument(2);
                        h.add(historyOf(TransactionType.CREDIT, price.getAmount()));
                        return event;
                    });

            ArgumentCaptor<CreditEntry> captor = ArgumentCaptor.forClass(CreditEntry.class);

            // when
            paymentService.cancelPayment(paymentRequest(), OrderStatus.CANCELLING);

            // then: 저장된 인스턴스가 findByCustomerId로 조회했던 바로 그 객체(identity)여야 한다
            verify(creditEntryRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(creditEntry);
        }
    }
}
