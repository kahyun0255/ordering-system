package com.orderingsystem.payment.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class CreditControllerGetBalanceTest extends ControllerTestSupport {

    @Autowired
    private CreditEntryRepository creditEntryRepository;

    @AfterEach
    void tearDown() {
        creditEntryRepository.deleteAllInBatch();
    }

    @DisplayName("계좌가 존재하면 잔액 조회에 성공한다.")
    @Test
    void shouldReturnBalance_whenAccountExists() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        CreditEntry creditEntry = CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .totalCreditAmount(new Money(BigDecimal.valueOf(10000)))
                .build();

        creditEntryRepository.save(creditEntry);

        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/accounts/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000));
    }

    @DisplayName("계좌가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccountDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/accounts/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("계좌가 존재하지 않습니다."));
    }

}
