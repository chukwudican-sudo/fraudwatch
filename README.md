# FraudWatch

[![Backend CI](https://github.com/chukwudican-sudo/fraudwatch/actions/workflows/ci.yml/badge.svg)](https://github.com/chukwudican-sudo/fraudwatch/actions/workflows/ci.yml)

Real-time fraud/anomaly-detection pipeline built as a 3-person Agile sprint (2 weeks, GitHub Projects board, daily standups).

## Team & Roles
- **Alex** — backend detection service
- **Daniel** — live dashboard (frontend)
- **Divine** — transaction simulator

## Architecture
Divine's simulator → Alex's detection service → Daniel's dashboard

## Repo Structure
This is a monorepo — each service lives in its own top-level folder and
is self-contained (its own dependency/build config, runnable on its own):
```
fraudwatch/
├── backend/     # Alex — detection service (Java / Spring Boot)
├── dashboard/   # Daniel — live dashboard (frontend)
└── simulator/   # Divine — transaction simulator
```

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
1. ✅ Unusual amount vs. account history — flags if an amount is more than 3x the account's historical average (once at least 3 past transactions exist)
2. ✅ Impossible travel — flags if the location differs from the account's most recent prior transaction and less than 2 minutes elapsed between them (no real distance/speed calculation, just a fixed time window; tightened from 30 minutes to match the simulator's pacing)
3. ✅ Unusual transaction frequency — flags if 5 or more of an account's past transactions fall within the 60 seconds immediately before this one (a sliding time window, not a fixed calendar window; tuned from 3-in-5-minutes to match the simulator's pacing)

*(All three detection rules are implemented.)*

## Running the Backend Locally

**Requirements:** Java 17+ and Maven (`brew install maven` on macOS).

All commands below are run from inside the `backend/` folder:
```bash
cd backend
```

1. **Run the server:**
   ```bash
   mvn spring-boot:run
   ```
   Maven downloads dependencies on first run, then starts the app at
   `http://localhost:8080`.

2. **Check it's alive:**
   ```bash
   curl http://localhost:8080/
   # {"status":"FraudWatch backend running"}
   ```

3. **Send a test transaction:**
   ```bash
   curl -X POST http://localhost:8080/transactions \
     -H "Content-Type: application/json" \
     -d '{"id":"tx1","timestamp":"2026-08-18T17:00:00Z","account_id":"acc1","amount":42.50,"location":"Toronto"}'
   ```

4. **Build a runnable jar** (optional):
   ```bash
   mvn package
   java -jar target/fraudwatch-backend-0.0.1-SNAPSHOT.jar
   ```

## Tests and CI

The backend has a JUnit 5 test suite in `backend/src/test/`:
- `FraudDetectionServiceTest`: each detection rule, including the exact
  boundaries (3x average, 2-minute travel window, 5-in-60-seconds burst)
  and which rule wins when several fire.
- `TransactionStoreTest`: per-account history and the newest-first,
  50-item dashboard feed.
- `TransactionControllerTest`: starts the Spring app and hits the REST
  endpoints, checking the JSON matches the data contract above.

Run them from `backend/`:
```bash
mvn test
```

GitHub Actions (`.github/workflows/ci.yml`) runs the same suite on every
push and pull request. The badge at the top of this README shows the
latest result.
