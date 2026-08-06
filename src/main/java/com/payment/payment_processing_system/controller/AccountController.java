package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.service.AccountService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for account read operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    /**
     * GET /api/accounts/{accountId}
     * Retrieve safe sender account details.
     *
     * @param accountId account identifier
     * @return 200 OK with safe account details
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable @Positive(message = "accountId must be a positive number") Long accountId) {
        log.info("GET /api/accounts/{} - Fetching account details", accountId);
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }
}

