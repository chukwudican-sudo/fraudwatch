# FraudWatch

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
2. ✅ Impossible travel — flags if the location differs from the account's most recent prior transaction and less than 30 minutes elapsed between them (no real distance/speed calculation, just a fixed time window)
3. Unusual transaction frequency in a short window

*(Rule 3 not implemented yet — those transactions still return `flagged: false`. Rules are being added one at a time.)*

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
