package com.fraudwatch.backend.store;

import com.fraudwatch.backend.model.TaggedTransaction;
import com.fraudwatch.backend.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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

    // How many tagged transactions the live dashboard feed shows. Older
    // ones are still kept in historyByAccount for the detection rules —
    // this limit only applies to what GET /transactions returns.
    private static final int RECENT_FEED_LIMIT = 50;

    // ConcurrentHashMap (not a plain HashMap) because Spring Boot can handle
    // several requests at once on different threads, and a plain HashMap
    // can corrupt itself if two threads write to it at the same time.
    // Same reasoning for CopyOnWriteArrayList below instead of ArrayList.
    private final Map<String, List<Transaction>> historyByAccount = new ConcurrentHashMap<>();

    // Every tagged transaction, across all accounts, in the order it was
    // saved. This is what powers the dashboard's live feed (GET
    // /transactions) — historyByAccount above is per-account and only
    // stores the raw (untagged) transaction, since that's all the
    // detection rules need to compare against.
    private final List<TaggedTransaction> recentTagged = new CopyOnWriteArrayList<>();

    public void save(Transaction transaction) {
        historyByAccount
                .computeIfAbsent(transaction.accountId(), key -> new CopyOnWriteArrayList<>())
                .add(transaction);
    }

    public List<Transaction> historyFor(String accountId) {
        return historyByAccount.getOrDefault(accountId, List.of());
    }

    public void recordTagged(TaggedTransaction tagged) {
        recentTagged.add(tagged);
    }

    /** The most recent tagged transactions, newest first, capped at RECENT_FEED_LIMIT. */
    public List<TaggedTransaction> recentTagged() {
        int size = recentTagged.size();
        int fromIndex = Math.max(0, size - RECENT_FEED_LIMIT);

        // subList() gives us oldest-first within that slice; reverse it
        // so the dashboard gets newest-first, like a typical activity feed.
        List<TaggedTransaction> newestFirst = new ArrayList<>(recentTagged.subList(fromIndex, size));
        Collections.reverse(newestFirst);
        return newestFirst;
    }
}
