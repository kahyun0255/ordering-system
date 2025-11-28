package com.orderingsystem.payment.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.presentation.request.CreditRequest;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CreditControllerWithdrawTest extends ControllerTestSupport {

    @Autowired
    private CreditEntryRepository creditEntryRepository;

    @Autowired
    private CreditHistoryRepository creditHistoryRepository;

    @AfterEach
    void tearDown() {
        creditEntryRepository.deleteAllInBatch();
        creditHistoryRepository.deleteAllInBatch();
    }

    @DisplayName("credit history에 문제가 없고, 기존 creditEntry가 존재하고 잔액이 출금 요청 잔액보다 많으면 출금시 기존 잔액에서 차감된다.")
    @Test
    void shouldWithdrawSuccessfully_whenCreditHistoryIsValid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(1000));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(totalCreditAmount)
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(100)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(BigDecimal.valueOf(900))).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(2L);
    }

    @DisplayName("출금할 계좌가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenCreditEntryNotFound() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(10000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("출금할 계좌가 존재하지 않습니다."));
    }

    @DisplayName("creditHistory가 creditEntry와 일치하지 않으면 출금에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenCreditHistoryDoesNotMatchEntry() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(100000));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(new Money(BigDecimal.valueOf(1000)))
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("[Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다.]"));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("잔액이 출금 요청 잔액보다 적으면 출금이 불가능하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenBalanceIsLessThanRequestedAmount() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(1000));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(totalCreditAmount)
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(10000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("잔액이 부족합니다."));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(BigDecimal.valueOf(1000))).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("출금할 금액이 null이면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenWithdrawAmountIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(100));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(totalCreditAmount)
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(null).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amount: amount는 필수입니다."));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("출금할 금액이 0이면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenWithdrawAmountIsZero() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(100));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(totalCreditAmount)
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(0)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amount: amount는 0보다 커야합니다."));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("출금할 금액이 음수면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenWithdrawAmountIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(100));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .amount(totalCreditAmount)
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(-1)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amount: amount는 0보다 커야합니다."));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

}
