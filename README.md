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

*(Not implemented yet — current backend always returns `flagged: false`. Rules are being added one at a time.)*

## Running the Backend Locally

**Requirements:** Java 17+ and Maven (`brew install maven` on macOS).

1. **Run the server** (from the project root):
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
