package com.orderingsystem.payment.presentation;

import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.payment.application.CreditService;
import com.orderingsystem.payment.application.dto.response.BalanceResponse;
import com.orderingsystem.payment.presentation.request.CreditRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class CreditController {

    private final CommonJwtUtil commonJwtUtil;
    private final CreditService creditService;

    @PostMapping("/deposit")
    public ResponseEntity<BalanceResponse> deposit(@RequestHeader("Authorization") String authorizationHeader,
                                                   @Valid @RequestBody CreditRequest creditRequest) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(creditService.deposit(userId, creditRequest.toApplicationRequest()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<BalanceResponse> withdraw(@RequestHeader("Authorization") String authorizationHeader,
                                                    @Valid @RequestBody CreditRequest creditRequest){
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(creditService.withdraw(userId, creditRequest.toApplicationRequest()));
    }

}
