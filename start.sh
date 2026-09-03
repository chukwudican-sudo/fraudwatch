#!/usr/bin/env bash
# Starts the whole FraudWatch stack: backend, simulator, dashboard.
#
# Cleans up anything already running first (same idea as stop.sh), then
# brings each service up in order, waiting for it to actually be ready
# before starting the next one - the backend must be up before the
# simulator has anything to POST to, and before the dashboard has
# anything to poll.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_LOG="/tmp/fraudwatch-backend.log"
SIMULATOR_LOG="/tmp/fraudwatch-simulator.log"
DASHBOARD_LOG="/tmp/fraudwatch-dashboard.log"

# --- cleanup -------------------------------------------------------------

kill_port() {
    local port="$1"
    local pids
    pids=$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null)
    if [ -n "$pids" ]; then
        kill $pids 2>/dev/null
        sleep 1
        pids=$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null)
        [ -n "$pids" ] && kill -9 $pids 2>/dev/null
    fi
}

kill_simulator() {
    local pids pid cwd matched=""
    pids=$(pgrep -f "main\.py" 2>/dev/null)
    for pid in $pids; do
        cwd=$(lsof -p "$pid" 2>/dev/null | awk '$4 == "cwd" {print $NF}')
        [[ "$cwd" == */fraudwatch/simulator ]] && matched="${matched} ${pid}"
    done
    if [ -n "$matched" ]; then
        kill $matched 2>/dev/null
        sleep 1
        kill -9 $matched 2>/dev/null
    fi
}

# Vite silently picks a different port if 5173 is taken, so also catch a
# drifted-port dashboard by working directory - scoped exactly to this
# project's dashboard/ folder so it can never touch someone else's
# unrelated "npm run dev" on the same machine.
kill_dashboard_by_cwd() {
    local pids pid cwd matched=""
    pids=$(pgrep -f "vite|npm run dev" 2>/dev/null)
    for pid in $pids; do
        cwd=$(lsof -p "$pid" 2>/dev/null | awk '$4 == "cwd" {print $NF}')
        [[ "$cwd" == */fraudwatch/dashboard ]] && matched="${matched} ${pid}"
    done
    if [ -n "$matched" ]; then
        kill $matched 2>/dev/null
        sleep 1
        kill -9 $matched 2>/dev/null
    fi
}

echo "== Cleaning up any existing services =="
kill_port 8080     # backend
kill_port 5173     # dashboard (Vite's default; also covers a leftover run)
kill_dashboard_by_cwd
kill_simulator
echo "done."
echo

# --- helpers ---------------------------------------------------------------

# Polls a URL until it responds or the timeout (seconds) elapses.
wait_for_http() {
    local url="$1"
    local timeout="$2"
    local waited=0
    while ! curl -sf "$url" >/dev/null 2>&1; do
        sleep 1
        waited=$((waited + 1))
        [ "$waited" -ge "$timeout" ] && return 1
    done
    return 0
}

transaction_count() {
    curl -s "http://localhost:8080/transactions" 2>/dev/null |
        python3 -c "import json,sys; print(len(json.load(sys.stdin)))" 2>/dev/null || echo 0
}

# --- backend -----------------------------------------------------------

echo "== Building backend =="
if ! (cd "$SCRIPT_DIR/backend" && mvn -q -DskipTests package); then
    echo "backend build failed - see output above"
    exit 1
fi

echo "== Starting backend (port 8080) =="
(
    cd "$SCRIPT_DIR/backend"
    nohup java -jar target/fraudwatch-backend-0.0.1-SNAPSHOT.jar >"$BACKEND_LOG" 2>&1 &
    echo $! >/tmp/fraudwatch-backend.pid
    disown
)
BACKEND_PID=$(cat /tmp/fraudwatch-backend.pid)

if wait_for_http "http://localhost:8080/" 30; then
    echo "backend is up (pid ${BACKEND_PID})"
else
    echo "backend did not come up within 30s - check ${BACKEND_LOG}"
    exit 1
fi
echo

# --- simulator -----------------------------------------------------------

echo "== Starting simulator =="
BEFORE_COUNT=$(transaction_count)

(
    cd "$SCRIPT_DIR/simulator"
    source venv/bin/activate
    nohup python main.py >"$SIMULATOR_LOG" 2>&1 &
    echo $! >/tmp/fraudwatch-simulator.pid
    disown
)
SIMULATOR_PID=$(cat /tmp/fraudwatch-simulator.pid)

if ! kill -0 "$SIMULATOR_PID" 2>/dev/null; then
    echo "simulator process exited immediately - check ${SIMULATOR_LOG}"
    exit 1
fi

sleep 6
AFTER_COUNT=$(transaction_count)

if [ "$AFTER_COUNT" -gt "$BEFORE_COUNT" ]; then
    echo "simulator is up and posting (pid ${SIMULATOR_PID})"
else
    echo "simulator process is running (pid ${SIMULATOR_PID}) but no new transactions showed up yet - check ${SIMULATOR_LOG}"
fi
echo

# --- dashboard ---------------------------------------------------------

echo "== Starting dashboard =="
(
    cd "$SCRIPT_DIR/dashboard"
    nohup npm run dev >"$DASHBOARD_LOG" 2>&1 &
    echo $! >/tmp/fraudwatch-dashboard.pid
    disown
)
DASHBOARD_PID=$(cat /tmp/fraudwatch-dashboard.pid)

DASHBOARD_URL=""
waited=0
while [ "$waited" -lt 30 ]; do
    DASHBOARD_URL=$(grep -oE 'Local:[[:space:]]+http://[a-zA-Z0-9.:/]+' "$DASHBOARD_LOG" 2>/dev/null | awk '{print $2}' | head -1)
    [ -n "$DASHBOARD_URL" ] && break
    sleep 1
    waited=$((waited + 1))
done

if [ -z "$DASHBOARD_URL" ]; then
    echo "dashboard did not report a URL within 30s - check ${DASHBOARD_LOG}"
    exit 1
fi

if wait_for_http "$DASHBOARD_URL" 15; then
    echo "dashboard is up (pid ${DASHBOARD_PID}) at ${DASHBOARD_URL}"
else
    echo "dashboard URL ${DASHBOARD_URL} did not respond within 15s - check ${DASHBOARD_LOG}"
    exit 1
fi
echo

# --- summary -------------------------------------------------------------

echo "== FraudWatch is running =="
echo "  backend    pid ${BACKEND_PID}    http://localhost:8080"
echo "  simulator  pid ${SIMULATOR_PID}    (no HTTP port - POSTs to the backend)"
echo "  dashboard  pid ${DASHBOARD_PID}    ${DASHBOARD_URL}"
echo
echo "Open ${DASHBOARD_URL} to watch the live feed."
echo "Logs: ${BACKEND_LOG}, ${SIMULATOR_LOG}, ${DASHBOARD_LOG}"
echo "Run ./stop.sh to stop everything."
