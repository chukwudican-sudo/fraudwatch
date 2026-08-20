package com.fraudwatch.backend.controller;

import com.fraudwatch.backend.model.TaggedTransaction;
import com.fraudwatch.backend.model.Transaction;
import com.fraudwatch.backend.store.TransactionStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Defines FraudWatch's two HTTP endpoints. {@code @RestController} tells
 * Spring that every method's return value should be serialized straight
 * to a JSON response body (equivalent to what FastAPI does by default).
 * Spring routes requests to these methods based on the {@code @GetMapping}
 * / {@code @PostMapping} annotations — no manual URL dispatch needed.
 */
@RestController
public class TransactionController {

    private final TransactionStore transactionStore;

    // Constructor injection: Spring sees this constructor needs a
    // TransactionStore and automatically passes in the single shared
    // instance it created at startup (see @Component on TransactionStore).
    public TransactionController(TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
    }

    @GetMapping("/")
    public Map<String, String> healthCheck() {
        return Map.of("status", "FraudWatch backend running");
    }

    @PostMapping("/transactions")
    public TaggedTransaction receiveTransaction(@RequestBody Transaction transaction) {
        // Record this transaction in that account's history so future
        // detection rules have past behavior to compare against.
        transactionStore.save(transaction);

        // No detection logic yet — every transaction comes back
        // flagged=false. We'll replace this with real rule checks in
        // later steps, one rule at a time, so each can be tested alone.
        return TaggedTransaction.from(transaction, false, "");
    }
}
