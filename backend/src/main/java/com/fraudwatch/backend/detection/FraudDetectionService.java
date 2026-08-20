package com.fraudwatch.backend.detection;

import com.fraudwatch.backend.model.Transaction;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Runs each incoming transaction through our fraud detection rules.
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

    // Shortest gap we consider realistic between two transactions in
    // different locations. We don't have real geographic distance
    // between locations, just names, so this is a simplification: ANY
    // location change faster than this window is treated as suspicious,
    // regardless of how far apart the two locations actually are.
    private static final Duration MIN_TRAVEL_TIME = Duration.ofMinutes(30);

    // How far back we look when counting an account's recent transactions.
    private static final Duration FREQUENCY_WINDOW = Duration.ofMinutes(5);

    // If this many (or more) of the account's past transactions fall
    // inside FREQUENCY_WINDOW, the new transaction gets flagged as part
    // of an unusually rapid burst.
    private static final int MAX_TRANSACTIONS_IN_WINDOW = 3;

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

        DetectionResult impossibleTravel = checkImpossibleTravel(transaction, pastTransactions);
        if (impossibleTravel.flagged()) {
            return impossibleTravel;
        }

        DetectionResult unusualFrequency = checkUnusualFrequency(transaction, pastTransactions);
        if (unusualFrequency.flagged()) {
            return unusualFrequency;
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

    private DetectionResult checkImpossibleTravel(Transaction transaction, List<Transaction> pastTransactions) {
        if (pastTransactions.isEmpty()) {
            // No prior transaction for this account to compare against yet.
            return DetectionResult.clear();
        }

        // historyFor() returns transactions in the order they were saved,
        // so the last element is the most recent one before this transaction.
        Transaction previous = pastTransactions.get(pastTransactions.size() - 1);

        if (previous.location().equals(transaction.location())) {
            // Same location as last time — no travel to question.
            return DetectionResult.clear();
        }

        // Instant.parse() reads an ISO 8601 timestamp like
        // "2026-08-20T10:00:00Z" into a single point in time. Duration.between()
        // gives us the elapsed time between two Instants as a Duration we can
        // compare directly — no manual date-math needed. We take the absolute
        // value in case timestamps ever arrive slightly out of order.
        Instant previousTime = Instant.parse(previous.timestamp());
        Instant currentTime = Instant.parse(transaction.timestamp());
        Duration timeBetween = Duration.between(previousTime, currentTime).abs();

        if (timeBetween.compareTo(MIN_TRAVEL_TIME) < 0) {
            return new DetectionResult(true, "Impossible travel: location changed too quickly");
        }

        return DetectionResult.clear();
    }

    private DetectionResult checkUnusualFrequency(Transaction transaction, List<Transaction> pastTransactions) {
        Instant currentTime = Instant.parse(transaction.timestamp());

        // Count how many past transactions fall inside the FREQUENCY_WINDOW
        // immediately before this one. For each past transaction we compute
        // the elapsed time up to the current transaction (Duration.between),
        // then keep only the ones where that gap is within the window —
        // this is the "sliding window" pattern: instead of a fixed calendar
        // window, the window always ends at the current transaction's time
        // and slides back FREQUENCY_WINDOW behind it.
        long recentCount = pastTransactions.stream()
                .map(past -> Duration.between(Instant.parse(past.timestamp()), currentTime).abs())
                .filter(gap -> gap.compareTo(FREQUENCY_WINDOW) <= 0)
                .count();

        if (recentCount >= MAX_TRANSACTIONS_IN_WINDOW) {
            return new DetectionResult(true, "Unusual transaction frequency: too many transactions in a short window");
        }

        return DetectionResult.clear();
    }
}
