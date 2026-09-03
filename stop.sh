#!/usr/bin/env bash
# Stops the FraudWatch backend, simulator, and dashboard dev server.
#
# Backend and dashboard are found by whatever's listening on their port
# (8080 / 5173) rather than a saved PID, so this works even if something
# was started outside of start.sh. The simulator doesn't listen on any
# port, so it's found by matching "main.py" processes whose working
# directory is simulator/ (not just any main.py on the machine).

set -uo pipefail

kill_port() {
    local port="$1"
    local label="$2"
    local pids
    pids=$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null)

    if [ -z "$pids" ]; then
        echo "  ${label}: nothing listening on port ${port}"
        return
    fi

    echo "  ${label}: stopping pid(s) ${pids} (port ${port})"
    kill $pids 2>/dev/null
    sleep 1

    # Anything that ignored SIGTERM gets a harder nudge.
    pids=$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null)
    if [ -n "$pids" ]; then
        kill -9 $pids 2>/dev/null
    fi
}

kill_simulator() {
    local pids matched pid cwd
    pids=$(pgrep -f "main\.py" 2>/dev/null)
    matched=""

    for pid in $pids; do
        cwd=$(lsof -p "$pid" 2>/dev/null | awk '$4 == "cwd" {print $NF}')
        if [[ "$cwd" == */fraudwatch/simulator ]]; then
            matched="${matched} ${pid}"
        fi
    done

    if [ -z "$matched" ]; then
        echo "  simulator: no matching main.py process running"
        return
    fi

    echo "  simulator: stopping pid(s)${matched}"
    kill $matched 2>/dev/null
    sleep 1
    kill -9 $matched 2>/dev/null
}

# The dashboard dev server (npm run dev -> vite) doesn't always end up on
# port 5173 - if that port was already taken, Vite silently picks the next
# free one. kill_port 5173 only catches the common case, so this also
# matches by working directory. Scoped to cwd == exactly this project's
# dashboard/ folder so it can never touch an unrelated "npm run dev" for
# some other project on the same machine.
kill_dashboard_by_cwd() {
    local pids matched pid cwd
    pids=$(pgrep -f "vite|npm run dev" 2>/dev/null)
    matched=""

    for pid in $pids; do
        cwd=$(lsof -p "$pid" 2>/dev/null | awk '$4 == "cwd" {print $NF}')
        if [[ "$cwd" == */fraudwatch/dashboard ]]; then
            matched="${matched} ${pid}"
        fi
    done

    if [ -z "$matched" ]; then
        return
    fi

    echo "  dashboard: also stopping pid(s)${matched} (not on port 5173)"
    kill $matched 2>/dev/null
    sleep 1
    kill -9 $matched 2>/dev/null
}

echo "Stopping FraudWatch services..."
kill_port 8080 "backend"
kill_port 5173 "dashboard"
kill_dashboard_by_cwd
kill_simulator
echo "Done."
