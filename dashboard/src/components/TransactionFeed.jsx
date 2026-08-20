import './TransactionFeed.css'

function formatAmount(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString([], { hour12: false })
}

export default function TransactionFeed({ transactions }) {
  return (
    <div className="feed">
      <div className="feed__head">
        <h2>Transaction feed</h2>
        <span className="feed__count">{transactions.length} shown</span>
      </div>

      <div className="feed__list">
        {transactions.length === 0 && <div className="feed__empty">Waiting on first transactions...</div>}

        {transactions.map((t) => (
          <div key={t.id} className={`feed__row ${t.flagged ? 'feed__row--flagged' : ''}`}>
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
              {t.flagged && <div className="feed__reason">flagged: {t.reason}</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
