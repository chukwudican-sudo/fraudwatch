"""Sends transactions to the detection service's /transactions endpoint.

Alex's backend may not be live yet (or may go down mid-sprint) — POST
failures are logged as warnings, not crashes, and every transaction is
always appended to a local JSONL file regardless of POST success, so you
always have a full record of what the simulator produced.
"""
import json

import requests

import config


def log_locally(txn: dict) -> None:
    with open(config.LOCAL_LOG_PATH, "a") as f:
        f.write(json.dumps(txn) + "\n")


def send_transaction(txn: dict) -> bool:
    """Returns True if the POST succeeded, False otherwise. Always logs
    locally first so nothing is lost on a failed POST."""
    log_locally(txn)
    try:
        response = requests.post(
            config.ENDPOINT_URL, json=txn, timeout=config.POST_TIMEOUT_SECONDS
        )
        if response.status_code >= 400:
            print(f"[poster] endpoint rejected transaction {txn['id']}: "
                  f"{response.status_code} {response.text[:200]}")
            return False
        return True
    except requests.exceptions.RequestException as e:
        print(f"[poster] could not reach endpoint ({config.ENDPOINT_URL}): {e}")
        return False
