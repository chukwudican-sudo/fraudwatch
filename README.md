# FraudWatch

Real-time fraud/anomaly-detection pipeline built as a 3-person Agile sprint (2 weeks, GitHub Projects board, daily standups).

## Team & Roles
- **Alex** — backend detection service
- **Daniel** — live dashboard (frontend)
- **Divine** — transaction simulator

## Architecture
Divine's simulator → Alex's detection service → Daniel's dashboard

## Data Contract

**Transaction** (sent by Divine's simulator):
```json
{ "id": "string", "timestamp": "ISO 8601", "account_id": "string", "amount": "number", "location": "string" }
```

**Tagged Output** (returned by Alex's detection service):
```json
{ "id": "same", "timestamp": "same", "account_id": "same", "amount": "same", "location": "same", "flagged": "boolean", "reason": "string" }
```

## Transport
HTTP: Divine's simulator POSTs transactions to Alex's API. Daniel's dashboard polls the API for results.

## Detection Rules
1. Unusual amount vs. account history
2. Impossible travel between two transaction locations given time elapsed
3. Unusual transaction frequency in a short window
