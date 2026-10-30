package com.fraudwatch.backend.detection;

import com.fraudwatch.backend.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the three detection rules. Each test builds a small,
 * hand-made transaction history, runs one new transaction through
 * FraudDetectionService, and checks the verdict.
 * <p>
 * No Spring, no server, no database: just plain Java objects, so these
 * run in milliseconds.
 */
class FraudDetectionServiceTest {

    private static final String AMOUNT_REASON = "Amount significantly higher than account average";
    private static final String TRAVEL_REASON = "Impossible travel: location changed too quickly";
    private static final String FREQUENCY_REASON =
            "Unusual transaction frequency: too many transactions in a short window";

    // Fixed "now" so every test is repeatable.
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");

    private final FraudDetectionService service = new FraudDetectionService();

    /** A transaction for account "acc1" that happened secondsAgo before NOW. */
    private static Transaction tx(double amount, String location, long secondsAgo) {
        return new Transaction(
                "tx-" + secondsAgo + "-" + location,
                NOW.minusSeconds(secondsAgo).toString(),
                "acc1",
                amount,
                location);
    }

    /** Three calm $100 purchases in Toronto, spaced 10 minutes apart. */
    private static List<Transaction> normalHistory() {
        return List.of(
                tx(100, "Toronto", 1800),
                tx(100, "Toronto", 1200),
                tx(100, "Toronto", 600));
    }

    // ---------------------------------------------------------------
    // Baseline
    // ---------------------------------------------------------------

    @Test
    void firstEverTransactionIsNotFlagged() {
        DetectionResult result = service.evaluate(tx(5000, "Toronto", 0), List.of());

        assertThat(result.flagged()).isFalse();
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void normalTransactionIsNotFlagged() {
        DetectionResult result = service.evaluate(tx(120, "Toronto", 0), normalHistory());

        assertThat(result.flagged()).isFalse();
    }

    // ---------------------------------------------------------------
    // Rule 1: unusual amount (more than 3x the account average)
    // ---------------------------------------------------------------

    @Test
    void amountMoreThanThreeTimesAverageIsFlagged() {
        // Average is $100, so anything above $300 should be flagged.
        DetectionResult result = service.evaluate(tx(301, "Toronto", 0), normalHistory());

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(AMOUNT_REASON);
    }

    @Test
    void amountExactlyThreeTimesAverageIsNotFlagged() {
        // The rule is "greater than 3x", so exactly $300 passes.
        DetectionResult result = service.evaluate(tx(300, "Toronto", 0), normalHistory());

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void amountRuleWaitsForAtLeastThreePastTransactions() {
        // Only 2 past transactions: not enough to trust the average yet,
        // so even a huge amount gets through.
        List<Transaction> shortHistory = List.of(
                tx(100, "Toronto", 1200),
                tx(100, "Toronto", 600));

        DetectionResult result = service.evaluate(tx(10_000, "Toronto", 0), shortHistory);

        assertThat(result.flagged()).isFalse();
    }

    // ---------------------------------------------------------------
    // Rule 2: impossible travel (new location within 2 minutes)
    // ---------------------------------------------------------------

    @Test
    void locationChangeWithinTwoMinutesIsFlagged() {
        List<Transaction> history = List.of(tx(100, "Toronto", 90));

        DetectionResult result = service.evaluate(tx(100, "Vancouver", 0), history);

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(TRAVEL_REASON);
    }

    @Test
    void locationChangeAfterExactlyTwoMinutesIsNotFlagged() {
        List<Transaction> history = List.of(tx(100, "Toronto", 120));

        DetectionResult result = service.evaluate(tx(100, "Vancouver", 0), history);

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void sameLocationShortlyAfterIsNotTravel() {
        List<Transaction> history = List.of(tx(100, "Toronto", 10));

        DetectionResult result = service.evaluate(tx(100, "Toronto", 0), history);

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void travelOnlyComparesAgainstTheMostRecentTransaction() {
        // History is in save order, so the LAST element is the most recent.
        // The account was in Vancouver 60s ago, then Toronto 30s ago. A new
        // Toronto purchase matches the most recent location, so it is not
        // flagged, even though Vancouver was also less than 2 minutes ago.
        List<Transaction> history = List.of(
                tx(100, "Vancouver", 60),
                tx(100, "Toronto", 30));

        DetectionResult result = service.evaluate(tx(100, "Toronto", 0), history);

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void outOfOrderTimestampsStillCountAsImpossibleTravel() {
        // The previous transaction's timestamp is 30s AFTER the new one
        // (it arrived late). The gap is still only 30s, so it's flagged.
        List<Transaction> history = List.of(tx(100, "Toronto", -30));

        DetectionResult result = service.evaluate(tx(100, "Montreal", 0), history);

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(TRAVEL_REASON);
    }

    // ---------------------------------------------------------------
    // Rule 3: unusual frequency (5+ past transactions in the last 60s)
    // ---------------------------------------------------------------

    @Test
    void fiveTransactionsInLastMinuteIsFlagged() {
        List<Transaction> burst = List.of(
                tx(50, "Toronto", 50),
                tx(50, "Toronto", 40),
                tx(50, "Toronto", 30),
                tx(50, "Toronto", 20),
                tx(50, "Toronto", 10));

        DetectionResult result = service.evaluate(tx(50, "Toronto", 0), burst);

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(FREQUENCY_REASON);
    }

    @Test
    void fourTransactionsInLastMinuteIsNotFlagged() {
        List<Transaction> busyButNormal = List.of(
                tx(50, "Toronto", 40),
                tx(50, "Toronto", 30),
                tx(50, "Toronto", 20),
                tx(50, "Toronto", 10));

        DetectionResult result = service.evaluate(tx(50, "Toronto", 0), busyButNormal);

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void transactionsOlderThanTheWindowAreNotCounted() {
        // 4 inside the window plus 3 just outside it (61-63s ago).
        List<Transaction> history = new ArrayList<>(List.of(
                tx(50, "Toronto", 63),
                tx(50, "Toronto", 62),
                tx(50, "Toronto", 61)));
        history.addAll(List.of(
                tx(50, "Toronto", 40),
                tx(50, "Toronto", 30),
                tx(50, "Toronto", 20),
                tx(50, "Toronto", 10)));

        DetectionResult result = service.evaluate(tx(50, "Toronto", 0), history);

        assertThat(result.flagged()).isFalse();
    }

    @Test
    void transactionExactlySixtySecondsAgoIsInsideTheWindow() {
        List<Transaction> burst = List.of(
                tx(50, "Toronto", 60),
                tx(50, "Toronto", 40),
                tx(50, "Toronto", 30),
                tx(50, "Toronto", 20),
                tx(50, "Toronto", 10));

        DetectionResult result = service.evaluate(tx(50, "Toronto", 0), burst);

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(FREQUENCY_REASON);
    }

    // ---------------------------------------------------------------
    // Rule order
    // ---------------------------------------------------------------

    @Test
    void amountRuleWinsWhenSeveralRulesWouldFire() {
        // Huge amount AND a new city 30s later: the amount rule runs
        // first, so its reason is the one reported.
        List<Transaction> history = List.of(
                tx(100, "Toronto", 1800),
                tx(100, "Toronto", 1200),
                tx(100, "Toronto", 30));

        DetectionResult result = service.evaluate(tx(5000, "Vancouver", 0), history);

        assertThat(result.flagged()).isTrue();
        assertThat(result.reason()).isEqualTo(AMOUNT_REASON);
    }
}
