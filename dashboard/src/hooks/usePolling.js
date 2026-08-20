import { useEffect, useRef, useState } from 'react'
import { fetchLatestTransactions } from '../lib/dataSource'

const MAX_FEED_LENGTH = 60
const MAX_CHART_POINTS = 40
const POLL_INTERVAL_MS = 2000

// Owns the "live" state of the dashboard: the running transaction feed
// and the flag-rate-over-time series the chart reads from. Everything
// here is UI-agnostic - components just consume the arrays it returns.
export function usePolling() {
  const [transactions, setTransactions] = useState([])
  const [chartData, setChartData] = useState([])
  const [isConnected, setIsConnected] = useState(true)
  const seenCount = useRef(0)
  const flaggedCount = useRef(0)

  useEffect(() => {
    let cancelled = false

    async function poll() {
      try {
        const batch = await fetchLatestTransactions()
        if (cancelled || batch.length === 0) return

        setIsConnected(true)

        setTransactions((prev) => [...batch].reverse().concat(prev).slice(0, MAX_FEED_LENGTH))

        batch.forEach((t) => {
          seenCount.current += 1
          if (t.flagged) flaggedCount.current += 1
        })

        const flagRate = seenCount.current === 0 ? 0 : (flaggedCount.current / seenCount.current) * 100

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

  return { transactions, chartData, isConnected }
}
