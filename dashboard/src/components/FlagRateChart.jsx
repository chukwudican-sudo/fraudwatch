import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceArea,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts'
import './FlagRateChart.css'

// The range the detection thresholds were tuned to produce. Above this and
// either something is genuinely going on, or a rule needs retuning.
const EXPECTED_LOW = 3
const EXPECTED_HIGH = 8

// Fixed, not derived from the data: a domain that recalculates rescales the
// whole chart whenever a spike scrolls in or out, which reads as a glitch.
// 30 clears the biggest bursts we've measured (~26%) without clipping, while
// leaving the usual 2-8% readings legible against the gridlines.
const Y_MAX = 30
const Y_TICKS = [0, 10, 20, 30]

const AXIS_TICK = {
  fill: '#6b7481',
  fontSize: 10.5,
  fontFamily: 'JetBrains Mono, ui-monospace, monospace'
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload || !payload.length) return null
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip__time">{label}</div>
      <div className="chart-tooltip__value">{payload[0].value}% of transactions flagged</div>
    </div>
  )
}

export default function FlagRateChart({ data }) {
  const latest = data.length ? data[data.length - 1].flagRate : 0
  const elevated = latest > EXPECTED_HIGH
  const accent = elevated ? 'var(--sig-elevated)' : 'var(--safe)'
  const stroke = elevated ? '#e5a83c' : '#34d399'

  return (
    <div className="chart">
      <div className="chart__head">
        <div className="chart__title">
          <h2>Flag rate</h2>
          <span className="chart__subtitle">
            Share of transactions flagged, sampled every 2s
          </span>
        </div>
        <div className="chart__live">
          <span className="chart__dot" />
          live
        </div>
      </div>

      <div className="chart__reading">
        <span className="chart__reading-num" style={{ color: accent }}>
          {latest}
        </span>
        <span className="chart__reading-unit">%</span>
        <span className="chart__reading-note" style={{ color: accent }}>
          {elevated ? `above the normal ${EXPECTED_LOW}–${EXPECTED_HIGH}%` : 'within normal range'}
        </span>
      </div>

      <div className="chart__plot">
        <ResponsiveContainer width="100%" height="100%">
          {/* right margin leaves room for the last time label, which is centred
              on the final point and would otherwise be cut in half */}
          <AreaChart data={data} margin={{ top: 8, right: 34, left: 0, bottom: 0 }}>
            <CartesianGrid vertical={false} stroke="var(--line-soft)" />
            <XAxis
              dataKey="time"
              axisLine={false}
              tickLine={false}
              minTickGap={64}
              tick={AXIS_TICK}
            />
            <YAxis
              domain={[0, Y_MAX]}
              ticks={Y_TICKS}
              tickFormatter={(value) => `${value}%`}
              width={38}
              axisLine={false}
              tickLine={false}
              tick={AXIS_TICK}
            />
            <ReferenceArea
              y1={EXPECTED_LOW}
              y2={EXPECTED_HIGH}
              fill="#34d399"
              fillOpacity={0.1}
              stroke="none"
            />
            <Tooltip content={<CustomTooltip />} cursor={{ stroke: 'var(--line)' }} />
            {/* Stroke only. A filled area reaches from the line down to zero,
                so at a high reading it floods the panel and buries the band. */}
            <Area
              type="monotone"
              dataKey="flagRate"
              stroke={stroke}
              strokeWidth={2.5}
              fill="none"
              dot={false}
              isAnimationActive={true}
              animationDuration={300}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="chart__legend">
        <span className="chart__legend-swatch" />
        <span>
          Green band is the normal range ({EXPECTED_LOW}–{EXPECTED_HIGH}%). Left is 80 seconds
          ago, right is now.
        </span>
      </div>
    </div>
  )
}
