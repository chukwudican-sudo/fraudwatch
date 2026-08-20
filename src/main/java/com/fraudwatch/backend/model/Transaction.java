package com.fraudwatch.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The shape of a transaction as sent by Divine's simulator. A Java
 * "record" is a compact way to declare a class that's just a bag of
 * fields — it auto-generates the constructor, getters, equals/hashCode,
 * and toString for us, similar to what a Pydantic model gave us for free.
 * <p>
 * Jackson (Spring's built-in JSON library) converts the incoming JSON
 * request body into one of these automatically, matching JSON keys to
 * field names.
 */
public record Transaction(
        String id,
        String timestamp,
        @JsonProperty("account_id") String accountId,
        double amount,
        String location
) {
}
