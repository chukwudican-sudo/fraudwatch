"""Baseline transaction generation and per-account profiles.

Each simulated account has a "normal" spending pattern (a home location and
a typical amount range) so that anomalies later have something real to
deviate from.
"""
import random
import uuid
from datetime import datetime, timezone

import config

# A modest pool of plausible city locations. Kept as simple strings to match
# the "location": "string" field in the data contract.
LOCATIONS = [
    "Toronto, ON", "Oshawa, ON", "Mississauga, ON", "Ottawa, ON",
    "Vancouver, BC", "Calgary, AB", "Montreal, QC", "Halifax, NS",
    "New York, NY", "Chicago, IL", "London, UK", "Tokyo, JP",
    "Sydney, AU", "Berlin, DE", "Sao Paulo, BR",
]


class AccountProfile:
    """Tracks a simulated account's normal behavior and recent activity."""

    def __init__(self, account_id: str):
        self.account_id = account_id
        self.home_location = random.choice(LOCATIONS)
        # Typical single-transaction amount for this account.
        self.avg_amount = round(random.uniform(15, 400), 2)
        self.std_dev = round(self.avg_amount * 0.25, 2)

        # Most recent transaction, used by anomaly injectors (e.g. to know
        # where/when this account "just was" for impossible-travel checks).
        self.last_location = self.home_location
        self.last_timestamp = now_iso()

    def normal_amount(self) -> float:
        amount = random.gauss(self.avg_amount, self.std_dev)
        return round(max(1.0, amount), 2)

    def normal_location(self) -> str:
        # Mostly home location, occasionally a nearby-ish alternate to keep
        # the baseline stream from being perfectly repetitive.
        #
        # This was 0.85 (a 15% alternate-location chance), which combined
        # with the backend's impossible-travel rule (any location change
        # within a few minutes gets flagged, no real distance data) meant
        # a large share of ordinary baseline traffic was randomly
        # "teleporting" and tripping that rule on its own, with no
        # anomaly involved. 0.95 (5%) cut that down but real per-account
        # cadence (~30s) is still well inside the detection window, so
        # even a 5% chance kept producing enough coincidental location
        # changes to dominate the flag rate. 0.98 (2%) keeps baseline
        # transactions close to home_location all but rarely.
        if random.random() < 0.98:
            return self.home_location
        return random.choice(LOCATIONS)


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def make_account_pool(n: int = None) -> dict:
    n = n or config.NUM_ACCOUNTS
    accounts = {}
    for i in range(n):
        acc_id = f"acct_{i:04d}"
        accounts[acc_id] = AccountProfile(acc_id)
    return accounts


def build_transaction(account: AccountProfile, amount: float = None,
                       location: str = None, timestamp: str = None) -> dict:
    """Builds a transaction dict matching the data contract, and updates
    the account's last-known location/timestamp so future anomaly checks
    (like impossible travel) have something to compare against."""
    txn = {
        "id": str(uuid.uuid4()),
        "timestamp": timestamp or now_iso(),
        "account_id": account.account_id,
        "amount": amount if amount is not None else account.normal_amount(),
        "location": location or account.normal_location(),
    }
    account.last_location = txn["location"]
    account.last_timestamp = txn["timestamp"]
    return txn


def generate_normal_transaction(accounts: dict) -> dict:
    account = random.choice(list(accounts.values()))
    return build_transaction(account)
