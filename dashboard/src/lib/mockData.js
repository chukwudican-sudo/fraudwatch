// Generates fake tagged transactions that match the exact shape Alex's
// /transactions endpoint will return, so the UI can be built and tested
// with zero dependency on the backend being live.
//
// Real shape (from README data contract):
// {
//   id: string,
//   timestamp: ISO 8601 string,
//   account_id: string,
//   amount: number,
//   location: string,
//   flagged: boolean,
//   reason: string
// }

const LOCATIONS = [
  'Toronto, CA',
  'Oshawa, CA',
  'New York, US',
  'London, UK',
  'Lagos, NG',
  'Singapore, SG',
  'Sao Paulo, BR',
  'Tokyo, JP',
  'Berlin, DE',
  'Dubai, AE'
]

const FLAG_REASONS = [
  'unusual amount vs. account history',
  'impossible travel distance',
  'unusual transaction frequency'
]

function randomFrom(list) {
  return list[Math.floor(Math.random() * list.length)]
}

function randomAccountId() {
  return `acct_${Math.floor(1000 + Math.random() * 9000)}`
}

function randomAmount(flagged) {
  // Flagged transactions skew toward larger, "weirder" amounts so the
  // visual pattern in the feed/chart reads clearly during a demo.
  const base = flagged ? 800 + Math.random() * 9200 : 5 + Math.random() * 400
  return Math.round(base * 100) / 100
}

let counter = 0

export function generateMockTransaction() {
  counter += 1
  const flagged = Math.random() < 0.18 // ~18% flag rate, tune as needed

  return {
    id: `txn_${Date.now()}_${counter}`,
    timestamp: new Date().toISOString(),
    account_id: randomAccountId(),
    amount: randomAmount(flagged),
    location: randomFrom(LOCATIONS),
    flagged,
    reason: flagged ? randomFrom(FLAG_REASONS) : ''
  }
}

// Simulates a batch coming back from a poll, the way Alex's endpoint
// might return "everything new since your last request".
export function generateMockBatch(min = 1, max = 3) {
  const n = Math.floor(min + Math.random() * (max - min + 1))
  return Array.from({ length: n }, generateMockTransaction)
}
