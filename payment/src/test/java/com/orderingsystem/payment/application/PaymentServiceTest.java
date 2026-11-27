package com.orderingsystem.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.mapper.PaymentDataMapper;
import com.orderingsystem.payment.application.outbox.OrderOutboxHelper;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.payment.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndCancelService;
import com.orderingsystem.payment.domain.service.PaymentValidateAndInitiateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentValidateAndInitiateService paymentValidateAndInitiateService;

    @Mock
    private CreditEntryRepository creditEntryRepository;

    @Mock
    private CreditHistoryRepository creditHistoryRepository;

    @Mock
    private OrderOutboxHelper orderOutboxHelper;

    @Mock
    private PaymentDataMapper paymentDataMapper;

    @Mock
    private PaymentValidateAndCancelService paymentValidateAndCancelService;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID sagaId;
    private UUID customerId;
    private Payment payment;
    private CreditEntry creditEntry;
    private List<CreditHistory> creditHistories;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        sagaId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .price(new Money(BigDecimal.valueOf(10_000)))
                .status(PaymentStatus.COMPLETED)
                .build();

        creditEntry = CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .totalCreditAmount(new Money(BigDecimal.valueOf(50_000)))
                .build();

        creditHistories = new java.util.ArrayList<>(
                List.of(CreditHistory.builder()
                        .id(UUID.randomUUID())
                        .customerId(customerId)
                        .orderId(orderId)
                        .amount(new Money(BigDecimal.valueOf(10_000)))
                        .build())
        );

        when(processedMessageRepository.insertIgnore(any(), any(), any())).thenReturn(1);
    }

    private PaymentRequest paymentRequestOf(OrderStatus ignored) {
        return PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(BigDecimal.valueOf(10_000))
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("cancelPayment() - orderStatus 분기")
    class CancelPaymentBranching {

        @Test
        @DisplayName("CANCELLING이면 validateAndCancel을 호출한다")
        void shouldCallValidateAndCancel_whenOrderStatusIsCancelling() {
            //given
            given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
            given(creditEntryRepository.findByCustomerId(customerId)).willReturn(Optional.of(creditEntry));
            given(creditHistoryRepository.findByCustomerId(customerId)).willReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            given(event.getPayment()).willReturn(payment);
            given(paymentValidateAndCancelService.validateAndCancel(any(), anyList(), anyList()))
                    .willReturn(event);

            //when
            paymentService.cancelPayment(paymentRequestOf(OrderStatus.CANCELLING), OrderStatus.CANCELLING);

            //then
            then(paymentValidateAndCancelService).should(times(1))
                    .validateAndCancel(any(), anyList(), anyList());
            then(paymentValidateAndCancelService).should(never())
                    .validateAndRefund(any(), anyList(), anyList());
        }

        @Test
        @DisplayName("REJECTING이면 validateAndRefund를 호출한다")
        void shouldCallValidateAndRefund_whenOrderStatusIsRejecting() {
            //given
            given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
            given(creditEntryRepository.findByCustomerId(customerId)).willReturn(Optional.of(creditEntry));
            given(creditHistoryRepository.findByCustomerId(customerId)).willReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            given(event.getPayment()).willReturn(payment);
            given(paymentValidateAndCancelService.validateAndRefund(any(), anyList(), anyList()))
                    .willReturn(event);

            //when
            paymentService.cancelPayment(paymentRequestOf(OrderStatus.REJECTING), OrderStatus.REJECTING);

            //then
            then(paymentValidateAndCancelService).should(times(1))
                    .validateAndRefund(any(), anyList(), anyList());
            then(paymentValidateAndCancelService).should(never())
                    .validateAndCancel(any(), anyList(), anyList());
        }

        @Test
        @DisplayName("CANCELLING/REJECTING이 아니면 NPE 대신 PaymentApplicationException을 던진다")
        void shouldThrowPaymentApplicationException_whenOrderStatusIsUnexpected() {
            //given
            PaymentRequest request = paymentRequestOf(OrderStatus.PENDING);

            //when, then
            assertThatThrownBy(()->paymentService.cancelPayment(request, OrderStatus.PENDING))
                    .isInstanceOf(PaymentApplicationException.class)
                    .isNotInstanceOf(NullPointerException.class);

            then(paymentRepository).should(never()).findByOrderId(any());
            then(paymentValidateAndCancelService).shouldHaveNoInteractions();
            then(orderOutboxHelper).shouldHaveNoInteractions();
        }

        @DisplayName("결제 상태가 FAILED이면 취소 도메인 서비스를 호출하지 ㅇ낳고 크레딧 조회화 Outbox 저장도 하지 않는다.")
        @Test
        void shouldSkipCancel_whenPaymentStatusIsFailed() {
            //given
            Payment failedPayment = Payment.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .customerId(customerId)
                    .price(new Money(BigDecimal.valueOf(1000)))
                    .status(PaymentStatus.FAILED)
                    .build();
            given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(failedPayment));

            //when
            paymentService.cancelPayment(paymentRequestOf(OrderStatus.CANCELLING), OrderStatus.CANCELLING);

            //then
            then(creditEntryRepository).should(never()).findByCustomerId(any());
            then(paymentValidateAndCancelService).shouldHaveNoInteractions();
            then(orderOutboxHelper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("completePayment() - 크레딧 반영은 failureMessages.isEmpty() 기준으로 결정된다")
    class CompletePaymentCreditPersistence {

        @Test
        @DisplayName("검증 성공(failureMessages 비어있음) 시 creditEntry에서 실제로 차감되고 명시적으로 save된다")
        void shouldSubtractCreditAndSaveExplicitly_whenValidationSucceeds() {
            //given
            when(creditEntryRepository.findByCustomerId(customerId)).thenReturn(Optional.of(creditEntry));
            when(creditHistoryRepository.findByCustomerId(customerId)).thenReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            when(event.getPayment()).thenReturn(payment);

            when(paymentValidateAndInitiateService.validateAndInitiate(any(), any(), anyList(), anyList(), any()))
                    .thenReturn(event);

            Money before = creditEntry.getTotalCreditAmount();

            //when
            paymentService.completePayment(paymentRequestOf(null));

            //then
            assertThat(creditEntry.getTotalCreditAmount())
                    .isEqualTo(before.subtract(new Money(BigDecimal.valueOf(10_000))));
            verify(creditEntryRepository, times(1)).save(creditEntry);
        }

        @Test
        @DisplayName("검증 실패(failureMessages 채워짐) 시 creditEntry는 건드리지 않는다")
        void shouldNotTouchCredit_whenValidationFails() {
            //given
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

            Money before = creditEntry.getTotalCreditAmount();

            //when
            paymentService.completePayment(paymentRequestOf(null));

            //then
            assertThat(creditEntry.getTotalCreditAmount()).isEqualTo(before);
            verify(creditEntryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelPayment() - 크레딧 환급도 항상 명시적으로 save된다")
    class CancelPaymentCreditPersistence {

        @Test
        @DisplayName("취소 성공 시 PaymentService가 creditEntry에 결제 금액을 더하고 명시적으로 save() 호출한다")
        void shouldSaveCreditEntryExplicitly_whenCancelSucceeds() {
            //given
            given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
            given(creditEntryRepository.findByCustomerId(customerId)).willReturn(Optional.of(creditEntry));
            given(creditHistoryRepository.findByCustomerId(customerId)).willReturn(creditHistories);

            PaymentEvent event = mock(PaymentEvent.class);
            given(event.getPayment()).willReturn(payment);
            given(paymentValidateAndCancelService.validateAndCancel(any(), anyList(), anyList()))
                    .willReturn(event);

            Money before = creditEntry.getTotalCreditAmount();

            //when
            paymentService.cancelPayment(paymentRequestOf(OrderStatus.CANCELLING), OrderStatus.CANCELLING);

            //then
            assertThat(creditEntry.getTotalCreditAmount())
                    .isEqualTo(before.add(new Money(BigDecimal.valueOf(10_000))));
            then(creditEntryRepository).should(times(1)).save(creditEntry);
        }
    }

}
