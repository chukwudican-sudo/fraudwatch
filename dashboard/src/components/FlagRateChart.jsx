import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import './FlagRateChart.css'

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload || !payload.length) return null
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip__time">{label}</div>
      <div className="chart-tooltip__value">{payload[0].value}% flagged</div>
    </div>
  )
}

export default function FlagRateChart({ data }) {
  const latest = data.length ? data[data.length - 1].flagRate : 0

  return (
    <div className="chart">
      <div className="chart__head">
        <h2>Flag rate</h2>
        <div className="chart__live">
          <span className="chart__dot" />
          live
        </div>
      </div>

      <div className="chart__reading">
        <span className="chart__reading-num">{latest}</span>
        <span className="chart__reading-unit">%</span>
      </div>

      <div className="chart__plot">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="flagFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#ff5c72" stopOpacity={0.35} />
                <stop offset="100%" stopColor="#ff5c72" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis dataKey="time" hide />
            <YAxis hide domain={[0, (max) => Math.max(20, Math.ceil(max * 1.3))]} />
            <Tooltip content={<CustomTooltip />} />
            <Area
              type="monotone"
              dataKey="flagRate"
              stroke="#ff5c72"
              strokeWidth={2}
              fill="url(#flagFill)"
              isAnimationActive={true}
              animationDuration={300}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
