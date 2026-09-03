import TransactionFeed from './components/TransactionFeed'
import FlagRateChart from './components/FlagRateChart'
import SignalBreakdown from './components/SignalBreakdown'
import StatsBar from './components/StatsBar'
import { usePolling } from './hooks/usePolling'
import './App.css'

export default function App() {
  const { transactions, chartData, isConnected, stats } = usePolling()

  return (
    <div className="app">
      <header className="app__header">
        <div className="app__brand">
          <span className="app__brand-mark" />
          <div className="app__brand-text">
            <span className="app__brand-name">FraudWatch</span>
            <span className="app__brand-sub">Live transaction monitor</span>
          </div>
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
        <aside className="app__side">
          <FlagRateChart data={chartData} />
          <SignalBreakdown transactions={transactions} />
        </aside>
      </main>
    </div>
  )
}
