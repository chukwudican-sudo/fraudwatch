# FraudWatch — Transaction Simulator

Generates a steady stream of realistic transactions for ~25 simulated
accounts, occasionally injecting deliberate anomalies, and POSTs each one
to the detection backend's `/transactions` endpoint.

## Setup

```bash
pip install -r requirements.txt
```

## Run

```bash
python main.py
```

Stop anytime with `Ctrl+C`. A summary (posted / failed / anomaly count)
prints on exit.

## Before the backend is live

`config.ENDPOINT_URL` defaults to `http://localhost:8000/transactions`.
Until Alex's FastAPI service is running, POSTs will fail — that's expected
and non-fatal. Every transaction is still written to `transactions.jsonl`
regardless, so you can inspect output and keep developing independently.

Once his endpoint is up, either edit `config.ENDPOINT_URL` directly, or run
with an environment variable:

```bash
FRAUDWATCH_ENDPOINT="http://<his-host>:8000/transactions" python main.py
```

## What it generates

Each transaction matches the shared data contract:

```json
{
  "id": "uuid",
  "timestamp": "ISO 8601",
  "account_id": "acct_0001",
  "amount": 42.50,
  "location": "Toronto, ON"
}
```

### Anomaly types (weighted random selection, ~8% of ticks)

- **Unusual amount** — 5x to 20x an account's normal average.
- **Impossible travel** — a location far from the account's last-known
  location, timestamped within seconds/minutes of that last transaction.
- **Unusual frequency** — a burst of 6-12 transactions for one account
  within a 30-second window.

## Files

- `main.py` — simulation loop
- `generator.py` — account profiles + baseline ("normal") transactions
- `anomalies.py` — the three anomaly injectors
- `poster.py` — HTTP POST + local JSONL fallback logging
- `config.py` — all tunables (endpoint, rate, anomaly probability, etc.)

## Tuning

Everything worth adjusting lives in `config.py` — transaction rate,
anomaly probability/weights, number of accounts, burst size, etc. No need
to touch the logic files for basic tweaks.
