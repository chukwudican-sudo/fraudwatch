"""Entry point: runs a continuous stream of mostly-normal transactions with
occasional injected anomalies, and sends each one to Alex's backend (or
just logs locally if the endpoint isn't up yet).

Run:
    python main.py

Stop with Ctrl+C.
"""
import random
import time

import anomalies
import config
import generator
import poster


def run():
    print(f"[main] starting simulator | endpoint={config.ENDPOINT_URL} "
          f"| accounts={config.NUM_ACCOUNTS} | anomaly_rate={config.ANOMALY_PROBABILITY} "
          f"| local_log={config.LOCAL_LOG_PATH}")
    accounts = generator.make_account_pool()

    sent, failed, anomalies_fired = 0, 0, 0

    try:
        while True:
            if random.random() < config.ANOMALY_PROBABILITY:
                batch = anomalies.inject_random_anomaly(accounts)
                anomalies_fired += 1
                kind_label = "anomaly-burst" if len(batch) > 1 else "anomaly"
                for txn in batch:
                    ok = poster.send_transaction(txn)
                    sent += ok
                    failed += not ok
                    print(f"[{kind_label}] {txn['account_id']} "
                          f"${txn['amount']:.2f} @ {txn['location']} "
                          f"({txn['timestamp']})")
            else:
                txn = generator.generate_normal_transaction(accounts)
                ok = poster.send_transaction(txn)
                sent += ok
                failed += not ok
                print(f"[normal] {txn['account_id']} ${txn['amount']:.2f} "
                      f"@ {txn['location']} ({txn['timestamp']})")

            time.sleep(random.uniform(
                config.MIN_INTERVAL_SECONDS, config.MAX_INTERVAL_SECONDS
            ))
    except KeyboardInterrupt:
        print(f"\n[main] stopped. posted={sent} failed_posts={failed} "
              f"anomaly_events={anomalies_fired} "
              f"(all transactions saved to {config.LOCAL_LOG_PATH})")


if __name__ == "__main__":
    run()
