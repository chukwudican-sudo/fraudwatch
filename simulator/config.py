import os

# --- Transport ---
# Point this at Alex's live endpoint once it exists. Until then, POSTs will
# fail gracefully and every transaction still gets logged locally so you can
# keep developing without being blocked.
ENDPOINT_URL = os.environ.get("FRAUDWATCH_ENDPOINT", "http://localhost:8080/transactions")
POST_TIMEOUT_SECONDS = 3

# --- Local fallback logging ---
LOCAL_LOG_PATH = "transactions.jsonl"

# --- Stream pacing ---
# Average seconds between transactions in the baseline stream.
MIN_INTERVAL_SECONDS = 0.5
MAX_INTERVAL_SECONDS = 2.0

# --- Accounts ---
NUM_ACCOUNTS = 25

# --- Anomaly injection ---
# Probability that any given "tick" produces an anomaly instead of (or in
# addition to, for frequency bursts) a normal transaction.
#
# Override it for a demo without editing this file, same as FRAUDWATCH_ENDPOINT:
#     FRAUDWATCH_ANOMALY_RATE=0.05 python main.py
#
# This isn't 1:1 with the resulting flag rate: a quarter of anomaly draws
# (unusual_frequency) inject a burst of 6-12 transactions at once, most of
# which get flagged, so each anomaly draw contributes several flags on
# average rather than one. At 0.08 this was producing a ~37-39% flag
# rate - unrealistic for a fraud demo, where real-world fraud is a small
# fraction of a percent. 0.012 was chosen by scaling that observed rate
# down proportionally to land near the middle of a 3-8% target range,
# still firing an anomaly roughly every ~1-2 minutes at the default
# tick pace so a demo doesn't sit idle waiting for one.
DEFAULT_ANOMALY_PROBABILITY = 0.012


def _read_anomaly_probability(default=DEFAULT_ANOMALY_PROBABILITY):
    """Reads FRAUDWATCH_ANOMALY_RATE, falling back to the default on anything
    unusable. A typo shouldn't kill the simulator halfway through a demo, so a
    bad value warns and carries on rather than raising."""
    raw = os.environ.get("FRAUDWATCH_ANOMALY_RATE")
    if raw is None:
        return default

    try:
        value = float(raw)
    except ValueError:
        print(f"[config] FRAUDWATCH_ANOMALY_RATE={raw!r} is not a number, "
              f"falling back to {default}")
        return default

    if not 0.0 <= value <= 1.0:
        print(f"[config] FRAUDWATCH_ANOMALY_RATE={value} is outside 0.0-1.0, "
              f"falling back to {default}")
        return default

    return value


ANOMALY_PROBABILITY = _read_anomaly_probability()

# Relative weights for which anomaly type gets picked when an anomaly fires.
ANOMALY_WEIGHTS = {
    "unusual_amount": 0.4,
    "impossible_travel": 0.35,
    "unusual_frequency": 0.25,
}

# Unusual amount: multiplier range applied to the account's normal average.
UNUSUAL_AMOUNT_MULTIPLIER_RANGE = (5, 20)

# Impossible travel: how close together (seconds) the two far-apart
# transactions land. Real travel between distant cities takes hours;
# anything under a few minutes is "impossible".
IMPOSSIBLE_TRAVEL_MAX_GAP_SECONDS = 180

# Unusual frequency: how many transactions to burst, and over what window.
FREQUENCY_BURST_COUNT_RANGE = (6, 12)
FREQUENCY_BURST_WINDOW_SECONDS = 30
