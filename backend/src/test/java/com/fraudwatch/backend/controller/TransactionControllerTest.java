package com.fraudwatch.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the REST API. Starts the real Spring app (store,
 * detection service and controller wired together) and sends HTTP
 * requests to it through MockMvc, checking the JSON that comes back
 * matches the data contract in the README.
 * <p>
 * The store is shared across tests in this class, so each test uses its
 * own account_id to stay independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String transactionJson(String id, String timestamp, String accountId,
                                          double amount, String location) {
        return """
                {"id":"%s","timestamp":"%s","account_id":"%s","amount":%s,"location":"%s"}
                """.formatted(id, timestamp, accountId, amount, location);
    }

    private void postTransaction(String json) throws Exception {
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    void healthCheckReportsRunning() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FraudWatch backend running"));
    }

    @Test
    void postReturnsTaggedTransactionMatchingTheContract() throws Exception {
        String json = transactionJson("tx1", "2026-08-20T12:00:00Z", "contract-acc", 42.5, "Toronto");

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tx1"))
                .andExpect(jsonPath("$.timestamp").value("2026-08-20T12:00:00Z"))
                .andExpect(jsonPath("$.account_id").value("contract-acc"))
                .andExpect(jsonPath("$.amount").value(42.5))
                .andExpect(jsonPath("$.location").value("Toronto"))
                .andExpect(jsonPath("$.flagged").value(false))
                .andExpect(jsonPath("$.reason").value(""));
    }

    @Test
    void largeAmountAfterHistoryIsFlaggedThroughTheApi() throws Exception {
        postTransaction(transactionJson("h1", "2026-08-20T11:30:00Z", "big-spender", 100, "Toronto"));
        postTransaction(transactionJson("h2", "2026-08-20T11:40:00Z", "big-spender", 100, "Toronto"));
        postTransaction(transactionJson("h3", "2026-08-20T11:50:00Z", "big-spender", 100, "Toronto"));

        String suspicious = transactionJson("h4", "2026-08-20T12:00:00Z", "big-spender", 5000, "Toronto");

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(suspicious))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andExpect(jsonPath("$.reason").value("Amount significantly higher than account average"));
    }

    @Test
    void feedReturnsNewestTransactionFirst() throws Exception {
        postTransaction(transactionJson("feed-1", "2026-08-20T12:00:00Z", "feed-acc", 20, "Toronto"));
        postTransaction(transactionJson("feed-2", "2026-08-20T12:10:00Z", "feed-acc", 20, "Toronto"));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("feed-2"))
                .andExpect(jsonPath("$[1].id").value("feed-1"));
    }
}
