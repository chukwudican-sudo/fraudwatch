import { SIGNALS, SIGNAL_ORDER, signalFor } from '../lib/signals'
import './SignalBreakdown.css'

// Derived purely from the transactions already on screen — no extra polling,
// no backend change. It doubles as the legend for the feed's colour coding.
export default function SignalBreakdown({ transactions }) {
  const counts = { amount: 0, travel: 0, frequency: 0 }
  const byAccount = new Map()

  for (const t of transactions) {
    const signal = signalFor(t.flagged ? t.reason : null)
    if (!signal) continue
    counts[signal] += 1

    const entry = byAccount.get(t.account_id) ?? { account: t.account_id, count: 0, signals: new Set() }
    entry.count += 1
    entry.signals.add(signal)
    byAccount.set(t.account_id, entry)
  }

  const flagged = counts.amount + counts.travel + counts.frequency
  const cleared = transactions.length - flagged

  // Scale the bars against a floor, not just the largest count — otherwise a
  // single flag fills the whole track and reads as "everything is this".
  const widest = Math.max(4, ...SIGNAL_ORDER.map((key) => counts[key]))

  const repeatOffenders = [...byAccount.values()].sort((a, b) => b.count - a.count).slice(0, 3)

  return (
    <div className="breakdown">
      <div className="breakdown__head">
        <h2>Why things get flagged</h2>
        <span className="breakdown__scope">last {transactions.length}</span>
      </div>

      <div className="breakdown__rules">
        {SIGNAL_ORDER.map((key) => (
          <div className="breakdown__rule" key={key}>
            <div className="breakdown__rule-top">
              <span className="breakdown__dot" style={{ background: SIGNALS[key].color }} />
              <span className="breakdown__label">{SIGNALS[key].label}</span>
              <span className="breakdown__count">{counts[key]}</span>
            </div>
            <div className="breakdown__track">
              <div
                className="breakdown__fill"
                style={{
                  width: `${(counts[key] / widest) * 100}%`,
                  background: SIGNALS[key].color
                }}
              />
            </div>
          </div>
        ))}
      </div>

      <div className="breakdown__accounts">
        <span className="breakdown__accounts-title">Accounts to watch</span>
        {repeatOffenders.length === 0 ? (
          <span className="breakdown__accounts-empty">Nothing flagged in this window</span>
        ) : (
          repeatOffenders.map((entry) => (
            <div className="breakdown__account" key={entry.account}>
              <span className="breakdown__account-dots">
                {[...entry.signals].map((key) => (
                  <span
                    key={key}
                    className="breakdown__dot"
                    style={{ background: SIGNALS[key].color }}
                  />
                ))}
              </span>
              <span className="breakdown__account-id">{entry.account}</span>
              <span className="breakdown__account-count">
                {entry.count} {entry.count === 1 ? 'flag' : 'flags'}
              </span>
            </div>
          ))
        )}
      </div>

      <div className="breakdown__foot">
        <span className="breakdown__foot-dot" />
        {cleared} cleared automatically
      </div>
    </div>
  )
}
