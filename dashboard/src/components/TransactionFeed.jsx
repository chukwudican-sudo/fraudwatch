import { SIGNALS, signalFor } from '../lib/signals'
import './TransactionFeed.css'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString([], { hour12: false })
}

// How close together (in ms) two flagged transactions for the same account
// need to be to count as one ongoing burst rather than two unrelated flags.
// Matches the backend's own frequency-detection window.
const BURST_GAP_MS = 60_000

// `transactions` is already sorted newest-first. Walk it once and chain
// adjacent entries into a group when they're flagged, belong to the same
// account, and land within BURST_GAP_MS of the previous entry in that group.
function groupTransactions(transactions) {
  const groups = []

  for (const t of transactions) {
    const currentGroup = groups[groups.length - 1]
    const lastInGroup = currentGroup?.[currentGroup.length - 1]

    const joinsBurst =
      lastInGroup &&
      t.flagged &&
      lastInGroup.flagged &&
      lastInGroup.account_id === t.account_id &&
      Math.abs(new Date(lastInGroup.timestamp) - new Date(t.timestamp)) <= BURST_GAP_MS

    if (joinsBurst) {
      currentGroup.push(t)
    } else {
      groups.push([t])
    }
  }

  return groups
}

function TransactionRow({ t }) {
  const signal = signalFor(t.flagged ? t.reason : null)

  return (
    <div className={`feed__row ${t.flagged ? 'feed__row--flagged' : ''}`}>
      <div className="feed__row-bar" />
      <div className="feed__row-main">
        <div className="feed__row-top">
          <span className="feed__account">{t.account_id}</span>
          <span className="feed__amount">{formatAmount(t.amount)}</span>
        </div>
        <div className="feed__row-bottom">
          <span className="feed__location">{t.location}</span>
          <span className="feed__time">{formatTime(t.timestamp)}</span>
        </div>
        {signal && (
          <div className="feed__reason" title={t.reason}>
            <AlertIcon />
            <span>{SIGNALS[signal].label}</span>
          </div>
        )}
      </div>
    </div>
  )
}

function BurstRow({ group }) {
  const [newest] = group
  const totalAmount = group.reduce((sum, t) => sum + t.amount, 0)
  const signals = [...new Set(group.map((t) => signalFor(t.reason)).filter(Boolean))]

  return (
    <div className="feed__row feed__row--flagged feed__row--burst">
      <div className="feed__row-bar" />
      <div className="feed__row-main">
        <div className="feed__row-top">
          <span className="feed__account">{newest.account_id}</span>
          <span className="feed__amount">{formatAmount(totalAmount)}</span>
        </div>
        <div className="feed__row-bottom">
          <span className="feed__location">{newest.location}</span>
          <span className="feed__time">{formatTime(newest.timestamp)}</span>
        </div>
        <div className="feed__reason">
          <AlertIcon />
          <span>{signals.length === 1 ? SIGNALS[signals[0]].label : 'Multiple signals'}</span>
          <span className="feed__burst-badge">{group.length} grouped</span>
        </div>
      </div>
    </div>
  )
}

function AlertIcon() {
  return (
    <svg className="feed__reason-icon" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path
        d="M8 2.6 14.2 13H1.8L8 2.6Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <path d="M8 6.6v3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="8" cy="11.4" r="0.85" fill="currentColor" />
    </svg>
  )
}

export default function TransactionFeed({ transactions }) {
  const groups = groupTransactions(transactions)

  return (
    <div className="feed">
      <div className="feed__head">
        <h2>Transaction feed</h2>
        <span className="feed__count">{transactions.length} shown</span>
      </div>

      <div className="feed__list">
        {transactions.length === 0 && <div className="feed__empty">Waiting on first transactions...</div>}

        {groups.map((group) =>
          group.length > 1 ? (
            <BurstRow key={`burst-${group[0].id}`} group={group} />
          ) : (
            <TransactionRow key={group[0].id} t={group[0]} />
          )
        )}
      </div>
    </div>
  )
}
