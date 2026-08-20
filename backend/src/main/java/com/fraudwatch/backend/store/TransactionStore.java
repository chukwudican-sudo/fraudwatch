package com.fraudwatch.backend.store;

import com.fraudwatch.backend.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds every transaction we've seen so far, grouped by account_id. This
 * is what future detection rules (unusual amount, impossible travel,
 * unusual frequency) will compare each new transaction against.
 * <p>
 * {@code @Component} tells Spring to create exactly one instance of this
 * class at startup and hand it to any other class that asks for it (see
 * TransactionController's constructor) — this is "dependency injection,"
 * Spring's way of wiring objects together instead of us calling
 * {@code new TransactionStore()} everywhere by hand.
 * <p>
 * This storage is just an in-memory map: it resets whenever the app
 * restarts. That's fine for now — a real database can replace this
 * later without any of the calling code needing to change, since it's
 * only ever accessed through save()/historyFor().
 */
@Component
public class TransactionStore {

    // ConcurrentHashMap (not a plain HashMap) because Spring Boot can handle
    // several requests at once on different threads, and a plain HashMap
    // can corrupt itself if two threads write to it at the same time.
    // Same reasoning for CopyOnWriteArrayList below instead of ArrayList.
    private final Map<String, List<Transaction>> historyByAccount = new ConcurrentHashMap<>();

    public void save(Transaction transaction) {
        historyByAccount
                .computeIfAbsent(transaction.accountId(), key -> new CopyOnWriteArrayList<>())
                .add(transaction);
    }

    public List<Transaction> historyFor(String accountId) {
        return historyByAccount.getOrDefault(accountId, List.of());
    }
}
