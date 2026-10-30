package com.fraudwatch.backend.store;

import com.fraudwatch.backend.model.TaggedTransaction;
import com.fraudwatch.backend.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the in-memory transaction store. */
class TransactionStoreTest {

    private final TransactionStore store = new TransactionStore();

    private static Transaction tx(String id, String accountId) {
        return new Transaction(id, "2026-08-20T12:00:00Z", accountId, 10.0, "Toronto");
    }

    @Test
    void historyIsKeptSeparatePerAccountInSaveOrder() {
        store.save(tx("a1", "alice"));
        store.save(tx("b1", "bob"));
        store.save(tx("a2", "alice"));

        assertThat(store.historyFor("alice")).extracting(Transaction::id).containsExactly("a1", "a2");
        assertThat(store.historyFor("bob")).extracting(Transaction::id).containsExactly("b1");
    }

    @Test
    void unknownAccountHasEmptyHistory() {
        assertThat(store.historyFor("nobody")).isEmpty();
    }

    @Test
    void recentFeedIsNewestFirst() {
        for (int i = 1; i <= 3; i++) {
            store.recordTagged(TaggedTransaction.from(tx("t" + i, "alice"), false, ""));
        }

        List<TaggedTransaction> feed = store.recentTagged();

        assertThat(feed).extracting(TaggedTransaction::id).containsExactly("t3", "t2", "t1");
    }

    @Test
    void recentFeedIsCappedAtFifty() {
        for (int i = 1; i <= 60; i++) {
            store.recordTagged(TaggedTransaction.from(tx("t" + i, "alice"), false, ""));
        }

        List<TaggedTransaction> feed = store.recentTagged();

        assertThat(feed).hasSize(50);
        assertThat(feed.get(0).id()).isEqualTo("t60");   // newest
        assertThat(feed.get(49).id()).isEqualTo("t11");  // oldest one still shown
    }
}
