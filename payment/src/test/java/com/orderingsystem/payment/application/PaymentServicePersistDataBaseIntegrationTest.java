package com.orderingsystem.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PaymentServicePersistDataBaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreditEntryRepository creditEntryRepository;

    @Autowired
    private CreditHistoryRepository creditHistoryRepository;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        creditEntryRepository.save(CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .totalCreditAmount(new Money(new BigDecimal("1000.00")))
                .build());

        creditHistoryRepository.save(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .type(TransactionType.CREDIT)
                .amount(new Money(new BigDecimal("1000.00")))
                .build());
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAllInBatch();
        creditHistoryRepository.deleteAllInBatch();
        creditEntryRepository.deleteAllInBatch();
    }

    private PaymentRequest paymentRequest(BigDecimal price, PaymentOrderStatus status) {
        return PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(price)
                .createdAt(Instant.now())
                .paymentOrderStatus(status)
                .build();
    }

    @DisplayName("payment는 COMPLETED로, creditEntry는 price만큼 차감되어 DB에 실제로 반영된다")
    @Test
    void shouldPersistSubtractedCreditEntry_whenCompletePaymentSucceeds() {
        //given
        long historyCountBefore = creditHistoryRepository.count();

        //when
        paymentService.completePayment(paymentRequest(new BigDecimal("25.00"), PaymentOrderStatus.PENDING));

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount())
                .isEqualTo(new Money(new BigDecimal("975.00")));

        assertThat(creditHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
    }

    @DisplayName("잔액과 결제금액이 정확히 같으면 잔액은 0으로 저장된다")
    @Test
    void shouldPersistZeroBalance_whenCompletePaymentPriceEqualsBalance() {
        //given, when
        paymentService.completePayment(paymentRequest(new BigDecimal("1000.00"), PaymentOrderStatus.PENDING));

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount()).isEqualTo(new Money(BigDecimal.ZERO));
    }

    @DisplayName("잔액 부족으로 실패하면 payment는 FAILED로 저장되지만 creditEntry/creditHistory는 전혀 건드리지 않는다")
    @Test
    void shouldNotTouchCredit_whenCompletePaymentFailsDueToInsufficientBalance() {
        //given
        long historyCountBefore = creditHistoryRepository.count();

        //when
        paymentService.completePayment(paymentRequest(new BigDecimal("100000.00"), PaymentOrderStatus.PENDING));

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount()).isEqualTo(new Money(new BigDecimal("1000.00")));

        assertThat(creditHistoryRepository.count()).isEqualTo(historyCountBefore);
    }

    @DisplayName("payment는 CANCELLED로, creditEntry는 환급되어 DB에 실제로 반영된다")
    @Test
    void shouldPersistRefundedCreditEntry_whenCancelPaymentSucceedsWithCancellingStatus() {
        //given
        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .price(new Money(new BigDecimal("25.00")))
                .status(PaymentStatus.COMPLETED)
                .build());
        long historyCountBefore = creditHistoryRepository.count();

        //when
        paymentService.cancelPayment(paymentRequest(new BigDecimal("25.00"), PaymentOrderStatus.CANCELLED),
                OrderStatus.CANCELLING);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount())
                .isEqualTo(new Money(new BigDecimal("1025.00")));

        assertThat(creditHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
    }

    @DisplayName("validateAndRefund 경로도 동일하게 creditEntry가 환급되어 저장된다")
    @Test
    void shouldPersistRefundedCreditEntry_whenCancelPaymentSucceedsWithRejectingStatus() {
        //given
        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .price(new Money(new BigDecimal("25.00")))
                .status(PaymentStatus.COMPLETED)
                .build());

        //when
        paymentService.cancelPayment(paymentRequest(new BigDecimal("25.00"), PaymentOrderStatus.CANCELLED),
                OrderStatus.REJECTING);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount())
                .isEqualTo(new Money(new BigDecimal("1025.00")));
    }

    @DisplayName("취소 검증이 실패하면 payment는 저장되지만 creditEntry/creditHistory는 전혀 건드리지 않는다")
    @Test
    void shouldNotTouchCredit_whenCancelPaymentValidationFails() {
        //given
        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .price(new Money(new BigDecimal("-25.00")))
                .status(PaymentStatus.COMPLETED)
                .build());
        long historyCountBefore = creditHistoryRepository.count();

        //when
        paymentService.cancelPayment(paymentRequest(new BigDecimal("-25.00"), PaymentOrderStatus.CANCELLED),
                OrderStatus.CANCELLING);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);
        assertThat(creditEntry).isPresent();
        assertThat(creditEntry.get().getTotalCreditAmount()).isEqualTo(new Money(new BigDecimal("1000.00")));

        assertThat(creditHistoryRepository.count()).isEqualTo(historyCountBefore);
    }

    @DisplayName("두 메서드 모두 여러 CreditHistory 후보 중 '마지막에 추가된 것' 딱 1건만 저장한다")
    @Test
    void shouldPersistOnlyTheLastAddedCreditHistory_whenBothMethodsExecuted() {
        //given
        long baseline = creditHistoryRepository.count();

        //when
        paymentService.completePayment(paymentRequest(new BigDecimal("25.00"), PaymentOrderStatus.PENDING));

        //then
        assertThat(creditHistoryRepository.count()).isEqualTo(baseline + 1);

        CreditHistory debitHistory = historyForThisOrder(TransactionType.DEBIT);
        assertThat(debitHistory.getAmount()).isEqualTo(new Money(new BigDecimal("25.00")));

        //when
        paymentService.cancelPayment(paymentRequest(new BigDecimal("25.00"), PaymentOrderStatus.CANCELLED),
                OrderStatus.CANCELLING);

        //then
        assertThat(creditHistoryRepository.count()).isEqualTo(baseline + 2);

        CreditHistory creditHistory = historyForThisOrder(TransactionType.CREDIT);
        assertThat(creditHistory.getAmount()).isEqualTo(new Money(new BigDecimal("25.00")));
    }

    private CreditHistory historyForThisOrder(TransactionType type) {
        List<CreditHistory> histories = creditHistoryRepository.findByCustomerId(customerId);
        return histories.stream()
                .filter(h -> orderId.equals(h.getOrderId()) && type == h.getType())
                .max(Comparator.comparing(CreditHistory::getPaidAt))
                .orElseThrow();
    }

}
