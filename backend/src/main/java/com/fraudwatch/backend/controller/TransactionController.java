package com.fraudwatch.backend.controller;

import com.fraudwatch.backend.detection.DetectionResult;
import com.fraudwatch.backend.detection.FraudDetectionService;
import com.fraudwatch.backend.model.TaggedTransaction;
import com.fraudwatch.backend.model.Transaction;
import com.fraudwatch.backend.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Defines FraudWatch's HTTP endpoints. {@code @RestController} tells
 * Spring that every method's return value should be serialized straight
 * to a JSON response body (equivalent to what FastAPI does by default).
 * Spring routes requests to these methods based on the {@code @GetMapping}
 * / {@code @PostMapping} annotations — no manual URL dispatch needed.
 */
@RestController
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionStore transactionStore;
    private final FraudDetectionService fraudDetectionService;

    // Constructor injection: Spring sees this constructor needs a
    // TransactionStore and a FraudDetectionService, and automatically
    // passes in the single shared instance of each it created at startup
    // (see @Component / @Service on those classes).
    public TransactionController(TransactionStore transactionStore, FraudDetectionService fraudDetectionService) {
        this.transactionStore = transactionStore;
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/")
    public Map<String, String> healthCheck() {
        return Map.of("status", "FraudWatch backend running");
    }

    @PostMapping("/transactions")
    public TaggedTransaction receiveTransaction(@RequestBody Transaction transaction) {
        // Fetch this account's history BEFORE saving the current
        // transaction, so the detection rules only ever compare against
        // genuinely past transactions — never against themselves.
        List<Transaction> pastTransactions = transactionStore.historyFor(transaction.accountId());
        DetectionResult result = fraudDetectionService.evaluate(transaction, pastTransactions);

        // Only save AFTER the check has run.
        transactionStore.save(transaction);

        TaggedTransaction tagged = TaggedTransaction.from(transaction, result.flagged(), result.reason());
        transactionStore.recordTagged(tagged);

        log.info("account_id={} amount={} flagged={} reason=\"{}\"",
                transaction.accountId(), transaction.amount(), result.flagged(), result.reason());

        return tagged;
    }

    @GetMapping("/transactions")
    public List<TaggedTransaction> recentTransactions() {
        // Powers the dashboard's live feed: it polls this endpoint and
        // expects transactions already tagged with a flagged/reason verdict,
        // newest first.
        return transactionStore.recentTagged();
    }
}
