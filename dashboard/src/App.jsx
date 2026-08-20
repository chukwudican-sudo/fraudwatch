import TransactionFeed from './components/TransactionFeed'
import FlagRateChart from './components/FlagRateChart'
import StatsBar from './components/StatsBar'
import { usePolling } from './hooks/usePolling'
import './App.css'

export default function App() {
  const { transactions, chartData, isConnected, stats } = usePolling()

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <div className="app__eyebrow">fraudwatch</div>
          <h1>Live transaction monitor</h1>
        </div>
        <div className="app__header-right">
          <StatsBar stats={stats} />
          <div className={`app__status ${isConnected ? 'app__status--ok' : 'app__status--down'}`}>
            <span className="app__status-dot" />
            {isConnected ? 'connected' : 'connection lost'}
          </div>
        </div>
      </header>

      <main className="app__grid">
        <TransactionFeed transactions={transactions} />
        <FlagRateChart data={chartData} />
      </main>
    </div>
  )
}
