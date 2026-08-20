import os

# --- Transport ---
# Point this at Alex's live endpoint once it exists. Until then, POSTs will
# fail gracefully and every transaction still gets logged locally so you can
# keep developing without being blocked.
ENDPOINT_URL = os.environ.get("FRAUDWATCH_ENDPOINT", "http://localhost:8000/transactions")
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
ANOMALY_PROBABILITY = 0.08

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
