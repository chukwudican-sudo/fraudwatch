import './StatsBar.css'

export default function StatsBar({ stats }) {
  const { total, flagged } = stats
  const clean = total - flagged

  return (
    <div className="stats-bar">
      <div className="stats-bar__item">
        <span className="stats-bar__value">{total}</span>
        <span className="stats-bar__label">total seen</span>
      </div>
      <div className="stats-bar__divider" />
      <div className="stats-bar__item">
        <span className="stats-bar__value stats-bar__value--flag">{flagged}</span>
        <span className="stats-bar__label">flagged</span>
      </div>
      <div className="stats-bar__divider" />
      <div className="stats-bar__item">
        <span className="stats-bar__value stats-bar__value--safe">{clean}</span>
        <span className="stats-bar__label">clean</span>
      </div>
    </div>
  )
}
