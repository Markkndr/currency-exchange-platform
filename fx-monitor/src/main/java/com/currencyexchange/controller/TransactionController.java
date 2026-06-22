package com.currencyexchange.controller;

import com.currencyexchange.dto.transactions.CreateTransactionRequestDTO;
import com.currencyexchange.dto.transactions.TransactionDTO;
import com.currencyexchange.entity.User;
import com.currencyexchange.service.TransactionService;
import com.currencyexchange.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) String type,
            Authentication authentication) {

        Long userId = currentUserId(authentication);
        List<TransactionDTO> transactions = (type != null && !type.isBlank())
                ? transactionService.getUserTransactionsByType(userId, type.toUpperCase())
                : transactionService.getUserTransactions(userId);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(transactionService.getUserTransaction(currentUserId(authentication), id));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @Valid @RequestBody CreateTransactionRequestDTO request,
            Authentication authentication) {

        Long userId = currentUserId(authentication);
        log.info("Create transaction request for user: {}", userId);
        TransactionDTO created = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Resolves the authenticated principal (an email) to the owning user's id.
     * Every endpoint scopes its work to this id so a user can only ever see or
     * create their own transactions.
     */
    private Long currentUserId(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }
}
