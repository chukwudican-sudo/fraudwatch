"""Anomaly injectors. Each function takes the account pool and returns
either a single transaction dict, or (for frequency bursts) a list of them,
deliberately deviating from that account's normal pattern.
"""
import random
from datetime import datetime, timedelta, timezone

import config
from generator import LOCATIONS, build_transaction, now_iso


def inject_unusual_amount(accounts: dict) -> dict:
    """Amount far outside the account's normal range, same location."""
    account = random.choice(list(accounts.values()))
    low, high = config.UNUSUAL_AMOUNT_MULTIPLIER_RANGE
    multiplier = random.uniform(low, high)
    spike_amount = round(account.avg_amount * multiplier, 2)
    return build_transaction(account, amount=spike_amount)


def inject_impossible_travel(accounts: dict) -> dict:
    """A transaction in a location far from the account's last-known
    location, timestamped too soon after that last transaction for the
    travel to be physically possible."""
    account = random.choice(list(accounts.values()))

    # Pick a location clearly different from wherever they last were.
    candidates = [loc for loc in LOCATIONS if loc != account.last_location]
    far_location = random.choice(candidates)

    # Anchor the new timestamp shortly after their last transaction.
    try:
        last_ts = datetime.fromisoformat(account.last_timestamp.replace("Z", "+00:00"))
    except ValueError:
        last_ts = datetime.now(timezone.utc)

    gap = random.randint(30, config.IMPOSSIBLE_TRAVEL_MAX_GAP_SECONDS)
    new_ts = last_ts + timedelta(seconds=gap)
    timestamp = new_ts.isoformat(timespec="seconds").replace("+00:00", "Z")

    return build_transaction(account, location=far_location, timestamp=timestamp)


def inject_unusual_frequency(accounts: dict) -> list:
    """A burst of many transactions for a single account crammed into a
    short time window — normal amounts/locations, abnormal frequency."""
    account = random.choice(list(accounts.values()))
    low, high = config.FREQUENCY_BURST_COUNT_RANGE
    burst_count = random.randint(low, high)

    start_ts = datetime.now(timezone.utc)
    burst = []
    for i in range(burst_count):
        offset = random.uniform(0, config.FREQUENCY_BURST_WINDOW_SECONDS)
        ts = start_ts + timedelta(seconds=offset)
        timestamp = ts.isoformat(timespec="seconds").replace("+00:00", "Z")
        burst.append(build_transaction(account, timestamp=timestamp))

    # Keep chronological order within the burst.
    burst.sort(key=lambda t: t["timestamp"])
    return burst


def inject_random_anomaly(accounts: dict) -> list:
    """Picks one anomaly type according to configured weights and returns
    a list of transaction(s) (always a list, even for single-transaction
    anomalies, so callers have one consistent shape to handle)."""
    kinds = list(config.ANOMALY_WEIGHTS.keys())
    weights = list(config.ANOMALY_WEIGHTS.values())
    kind = random.choices(kinds, weights=weights, k=1)[0]

    if kind == "unusual_amount":
        return [inject_unusual_amount(accounts)]
    elif kind == "impossible_travel":
        return [inject_impossible_travel(accounts)]
    else:
        return inject_unusual_frequency(accounts)
