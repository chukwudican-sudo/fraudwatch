// One place that maps the backend's reason string onto a detection rule, so
// the feed and the breakdown panel always agree on wording.
//
// The reason strings come from FraudDetectionService and are matched loosely
// on a keyword, so rewording the sentence on the backend doesn't break the UI.
//
// Colour: every flagged transaction is the same red in the feed — a flag is a
// flag. The shades below are only used in the breakdown, where the three rules
// sit side by side and are ordered by how serious they are.

export const SIGNALS = {
  travel: { label: 'Location changed too fast', color: 'var(--sev-1)' },
  amount: { label: 'Larger than usual', color: 'var(--sev-2)' },
  frequency: { label: 'Too many at once', color: 'var(--sev-3)' }
}

// Most serious first. Impossible travel is the strongest fraud signal of the
// three: a card cannot be in two cities at once. A burst is the weakest —
// people do legitimately buy several things in a row.
export const SIGNAL_ORDER = ['travel', 'amount', 'frequency']

export function signalFor(reason) {
  if (!reason) return null
  const text = reason.toLowerCase()
  if (text.includes('frequency')) return 'frequency'
  if (text.includes('travel')) return 'travel'
  if (text.includes('amount')) return 'amount'
  return null
}
