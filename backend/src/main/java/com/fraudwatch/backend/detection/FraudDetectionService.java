package com.fraudwatch.backend.detection;

import com.fraudwatch.backend.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs each incoming transaction through our fraud detection rules.
 * More rules (impossible travel, unusual frequency) will be added here
 * later as their own private check methods, called from evaluate().
 */
@Service
public class FraudDetectionService {

    // Minimum number of past transactions needed before we trust an
    // account's average enough to compare against. With too few data
    // points the average is meaningless — e.g. a single $5 transaction
    // as the "average" would flag almost anything after it as unusual.
    private static final int MIN_HISTORY_SIZE = 3;

    // How many times an account's average amount a new transaction has
    // to be before we call it unusual. Arbitrary starting point — we can
    // tune this once we see how real/simulated data behaves.
    private static final double UNUSUAL_AMOUNT_MULTIPLIER = 3.0;

    /**
     * @param transaction     the incoming transaction being checked
     * @param pastTransactions that account's history BEFORE this transaction
     *                        (the caller must not have saved it yet)
     */
    public DetectionResult evaluate(Transaction transaction, List<Transaction> pastTransactions) {
        DetectionResult unusualAmount = checkUnusualAmount(transaction, pastTransactions);
        if (unusualAmount.flagged()) {
            return unusualAmount;
        }

        return DetectionResult.clear();
    }

    private DetectionResult checkUnusualAmount(Transaction transaction, List<Transaction> pastTransactions) {
        if (pastTransactions.size() < MIN_HISTORY_SIZE) {
            // Not enough history to establish a baseline yet — let it through.
            return DetectionResult.clear();
        }

        double averagePastAmount = pastTransactions.stream()
                .mapToDouble(Transaction::amount)
                .average()
                .orElse(0.0);

        if (transaction.amount() > averagePastAmount * UNUSUAL_AMOUNT_MULTIPLIER) {
            return new DetectionResult(true, "Amount significantly higher than account average");
        }

        return DetectionResult.clear();
    }
}
