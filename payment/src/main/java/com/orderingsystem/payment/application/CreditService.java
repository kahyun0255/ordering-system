package com.orderingsystem.payment.application;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.application.dto.request.CreditApplicationRequest;
import com.orderingsystem.payment.application.dto.response.BalanceResponse;
import com.orderingsystem.payment.domain.event.CreditEvent;
import com.orderingsystem.payment.domain.exception.PaymentDomainException;
import com.orderingsystem.payment.domain.exception.PaymentNotFoundException;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.service.CreditDepositService;
import com.orderingsystem.payment.domain.service.CreditWithdrawService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditEntryRepository creditEntryRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final CreditDepositService creditDepositService;
    private final CreditWithdrawService creditWithdrawService;

    @Transactional
    public BalanceResponse deposit(UUID userId, CreditApplicationRequest request) {
        log.info("[{}] 유저가 [{}] 금액 입금 요청.", userId, request.getAmount());

        CreditEntry entry = creditEntryRepository.findByCustomerId(userId)
                .orElseGet(() -> creditEntryRepository.save(CreditEntry.create(userId)));

        Money money = new Money(request.getAmount());
        List<CreditHistory> creditHistories = creditHistoryRepository.findByCustomerId(userId);
        List<String> failureMessages = new ArrayList<>();

        CreditEvent creditEvent = creditDepositService.deposit(entry, money, creditHistories, failureMessages);

        if (failureMessages.isEmpty()) {
            creditEntryRepository.save(creditEvent.getCreditEntry());
            creditHistoryRepository.save(creditEvent.getCreditHistory());
        } else {
            log.warn("크레딧 입금 검증 실패. userId : [{}], failureMessage : [{}]", userId, failureMessages.toString());
            throw new PaymentDomainException(failureMessages.toString());
        }

        log.info("[{}] 유저에게 [{}] 금액 입금 완료.", userId, request.getAmount());

        return BalanceResponse.builder()
                .balance(creditEvent.getCreditEntry().getTotalCreditAmount().getAmount())
                .build();
    }

    @Transactional
    public BalanceResponse withdraw(UUID userId, CreditApplicationRequest request) {
        log.info("[{}] 유저가 [{}] 금액 출금 요청.", userId, request.getAmount());

        CreditEntry entry = creditEntryRepository.findByCustomerId(userId)
                .orElseThrow(() -> new PaymentNotFoundException("출금할 계좌가 존재하지 않습니다."));

        Money money = new Money(request.getAmount());
        List<CreditHistory> creditHistories = creditHistoryRepository.findByCustomerId(userId);
        List<String> failureMessages = new ArrayList<>();

        CreditEvent creditEvent = creditWithdrawService.withdraw(entry, money, creditHistories, failureMessages);

        if (failureMessages.isEmpty()) {
            creditEntryRepository.save(creditEvent.getCreditEntry());
            creditHistoryRepository.save(creditEvent.getCreditHistory());
        } else {
            log.warn("크레딧 출금 검증 실패. userId : [{}], failureMessage : [{}]", userId, failureMessages.toString());
            throw new PaymentDomainException(failureMessages.toString());
        }

        log.info("[{}] 유저의 [{}] 금액 출금 완료.", userId, request.getAmount());

        return BalanceResponse.builder()
                .balance(creditEvent.getCreditEntry().getTotalCreditAmount().getAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId) {
        log.info("[{}] 유저가 잔액 조회를 요청.", userId);

        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(userId);
        if (creditEntry.isEmpty()){
            log.info("[{}] 유저의 계좌가 존재하지 않습니다.", userId);
            throw new PaymentNotFoundException("계좌가 존재하지 않습니다.");
        }

        return BalanceResponse.builder()
                .balance(creditEntry.get().getTotalCreditAmount().getAmount())
                .build();
    }

}
