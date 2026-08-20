package com.fraudwatch.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The shape of the response we send back to Daniel's dashboard: every
 * field from the original Transaction, plus our detection verdict.
 * Kept as its own record (rather than Transaction + extra fields bolted
 * on) so the JSON we send back always matches the agreed contract exactly.
 */
public record TaggedTransaction(
        String id,
        String timestamp,
        @JsonProperty("account_id") String accountId,
        double amount,
        String location,
        boolean flagged,
        String reason
) {

    /** Builds a tagged response from an incoming transaction plus a verdict. */
    public static TaggedTransaction from(Transaction transaction, boolean flagged, String reason) {
        return new TaggedTransaction(
                transaction.id(),
                transaction.timestamp(),
                transaction.accountId(),
                transaction.amount(),
                transaction.location(),
                flagged,
                reason
        );
    }
}
