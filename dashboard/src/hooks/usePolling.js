import { useEffect, useRef, useState } from 'react'
import { fetchLatestTransactions } from '../lib/dataSource'

const MAX_FEED_LENGTH = 60
const MAX_CHART_POINTS = 40
const POLL_INTERVAL_MS = 2000

export function usePolling() {
  const [transactions, setTransactions] = useState([])
  const [chartData, setChartData] = useState([])
  const [isConnected, setIsConnected] = useState(true)
  const [stats, setStats] = useState({ total: 0, flagged: 0 })
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

        setStats({ total: seenCount.current, flagged: flaggedCount.current })

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

  return { transactions, chartData, isConnected, stats }
}
