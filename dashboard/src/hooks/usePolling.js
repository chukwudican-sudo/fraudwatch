import { useEffect, useRef, useState } from 'react'
import { fetchLatestTransactions } from '../lib/dataSource'

const MAX_FEED_LENGTH = 60
const MAX_CHART_POINTS = 40
const POLL_INTERVAL_MS = 2000

// How many of the most recent transactions the flag-rate chart/stat looks
// at. Deliberately NOT all-time: an all-time average barely moves once
// enough transactions have accumulated, so it can't show a burst
// happening (or ending) the way a live monitor should. 50 matches the
// backend's own GET /transactions cap, so this lines up with what the
// API actually considers "recent."
const RECENT_WINDOW_SIZE = 50

// Each poll returns a snapshot of the most recent transactions from the
// backend, not just new ones, so the same transaction can legitimately
// reappear across polls. We merge by id into a Map to avoid duplicates
// and correctly track running totals, then derive the feed and chart
// from that Map on every poll.
export function usePolling() {
  const [transactions, setTransactions] = useState([])
  const [chartData, setChartData] = useState([])
  const [isConnected, setIsConnected] = useState(true)
  const [stats, setStats] = useState({ total: 0, flagged: 0 })
  const seenTransactions = useRef(new Map())

  useEffect(() => {
    let cancelled = false

    async function poll() {
      try {
        const snapshot = await fetchLatestTransactions()
        if (cancelled) return

        setIsConnected(true)

        snapshot.forEach((t) => {
          seenTransactions.current.set(t.id, t)
        })

        const all = Array.from(seenTransactions.current.values()).sort(
          (a, b) => new Date(b.timestamp) - new Date(a.timestamp)
        )

        setTransactions(all.slice(0, MAX_FEED_LENGTH))

        // Stats bar: genuinely cumulative, all-time counts — "how many
        // transactions has this session seen in total" is meant to keep
        // growing, so it stays derived from the full `all` array.
        const total = seenTransactions.current.size
        const flagged = all.filter((t) => t.flagged).length
        setStats({ total, flagged })

        // Flag rate: a RECENT rate, not cumulative. `all` is already
        // sorted newest-first, so the first RECENT_WINDOW_SIZE entries
        // are "what just happened." This is what lets the chart rise
        // during a burst and fall back down again once those flagged
        // transactions age out of the window, instead of creeping
        // toward one number as more history piles up.
        const recentWindow = all.slice(0, RECENT_WINDOW_SIZE)
        const recentFlagged = recentWindow.filter((t) => t.flagged).length
        const flagRate = recentWindow.length === 0 ? 0 : (recentFlagged / recentWindow.length) * 100

        setChartData((prev) => {
          const next = [
            ...prev,
            {
              time: new Date().toLocaleTimeString([], { hour12: false }),
              flagRate: Math.round(flagRate * 10) / 10
            }
          ]
          return next.slice(-MAX_CHART_POINTS)
        })
      } catch (err) {
        if (!cancelled) setIsConnected(false)
      }
    }

    poll()
    const id = setInterval(poll, POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [])

  return { transactions, chartData, isConnected, stats }
}
