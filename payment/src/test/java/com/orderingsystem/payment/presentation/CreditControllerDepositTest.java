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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CreditControllerDepositTest extends ControllerTestSupport {

    @Autowired
    private CreditEntryRepository creditEntryRepository;

    @Autowired
    private CreditHistoryRepository creditHistoryRepository;

    @AfterEach
    void tearDown() {
        creditEntryRepository.deleteAllInBatch();
        creditHistoryRepository.deleteAllInBatch();
    }

    @DisplayName("credit history에 문제가 없고, 기존 creditEntry가 존재하면 입금시 기존 잔액에서 추가된다.")
    @Test
    void shouldDepositSuccessfully_whenCreditHistoryIsValid() throws Exception {
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

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2000));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(BigDecimal.valueOf(2000))).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(2L);
    }

    @DisplayName("credit history에 문제가 없고, 기존 creditEntry가 존재하지 않으면 새로 creditEntry가 생성되고 요청한 잔액이 입금된다.")
    @Test
    void shouldCreateCreditEntryAndDeposit_whenNoExistingEntryAndValidHistory() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000));

        List<CreditEntry> creditEntries = creditEntryRepository.findAll();
        assertThat(creditEntries.size()).isOne();
        assertThat(creditEntries.get(0).getTotalCreditAmount().getAmount()
                .compareTo(BigDecimal.valueOf(1000))).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("잔액이 음수라면 입금에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenAmountIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(-11));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.DEBIT)
                .paidAt(ZonedDateTime.now())
                .amount(new Money(BigDecimal.valueOf(11)))
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("[Credit History 기준으로 크레딧이 부족합니다.]"));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("creditHistory가 creditEntry와 일치하지 않으면 입금에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenCreditHistoryDoesNotMatchEntry() throws Exception {
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
                .amount(new Money(BigDecimal.valueOf(1000)))
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/deposit")
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

    @DisplayName("creditHistory가 creditEntry와 일치하지 않으며 creditEntry가 음수라면 입금에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenCreditHistoryMismatchAndEntryNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Money totalCreditAmount = new Money(BigDecimal.valueOf(-100));
        CreditEntry creditEntry = CreditEntry.builder()
                .customerId(userId)
                .id(UUID.randomUUID())
                .totalCreditAmount(totalCreditAmount)
                .build();
        creditEntryRepository.save(creditEntry);

        CreditHistory creditHistory = CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .type(TransactionType.DEBIT)
                .paidAt(ZonedDateTime.now())
                .amount(new Money(BigDecimal.valueOf(1000)))
                .orderId(null)
                .build();
        creditHistoryRepository.save(creditHistory);

        String token = buildToken(userId);

        CreditRequest request = CreditRequest.builder().amount(BigDecimal.valueOf(1000)).build();

        //when, then
        mockMvc.perform(post("/api/accounts/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "[Credit History 기준으로 크레딧이 부족합니다., Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다.]"));

        Optional<CreditEntry> afterCreditEntry = creditEntryRepository.findById(creditEntry.getId());
        assertThat(afterCreditEntry.get().getTotalCreditAmount().getAmount()
                .compareTo(totalCreditAmount.getAmount())).isZero();
        assertThat(creditHistoryRepository.count()).isEqualTo(1L);
    }

    @DisplayName("입금할 금액이 null이면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDepositAmountIsNull() throws Exception {
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
        mockMvc.perform(post("/api/accounts/deposit")
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

    @DisplayName("입금할 금액이 0이면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDepositAmountIsZero() throws Exception {
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
        mockMvc.perform(post("/api/accounts/deposit")
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

    @DisplayName("입금할 금액이 음수면 입금에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDepositAmountIsNegative() throws Exception {
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
        mockMvc.perform(post("/api/accounts/deposit")
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
