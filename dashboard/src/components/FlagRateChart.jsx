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

// The y-axis grows to fit the data, but only in steps of 10.
//
// The original version of this chart set the ceiling to `max * 1.3`, which is a
// smooth function of the peak — so it changed slightly on *every* poll and
// Recharts animated the entire line between two different scales each time,
// which is what read as the chart "jumping". Snapping the ceiling to a multiple
// of 10 fixes that: between boundary crossings the axis is bit-for-bit
// identical, so the line simply extends, and a rescale only happens on a
// genuine crossing (rare) where it's meaningful.
const Y_STEP = 10

// Keeps the whole normal operating range (roughly 2-8%) inside the first band,
// so the axis never moves during ordinary traffic — crossings start at ~18%.
const Y_MIN_CEILING = 20

// Headroom below the ceiling, so a peak never sits flat against the top edge
// (which reads as clipping, and hid a real spike before).
const Y_HEADROOM = 3

function axisCeiling(data) {
  const peak = data.reduce((max, point) => Math.max(max, point.flagRate ?? 0), 0)
  let ceiling = Math.ceil(peak / Y_STEP) * Y_STEP
  if (ceiling - peak < Y_HEADROOM) ceiling += Y_STEP
  return Math.max(Y_MIN_CEILING, ceiling)
}

function axisTicks(ceiling) {
  const ticks = []
  for (let value = 0; value <= ceiling; value += Y_STEP) ticks.push(value)
  return ticks
}

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
  const ceiling = axisCeiling(data)
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
              domain={[0, ceiling]}
              ticks={axisTicks(ceiling)}
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
