// Single point of contact between the UI and "wherever transactions come
// from." Right now that's the mock generator. Once Alex's endpoint is
// live, this is the only file that needs to change - every component
// keeps calling fetchLatestTransactions() exactly the same way.

import { generateMockBatch } from './mockData'

const USE_MOCK = false // flip to false once API_BASE_URL is real

// Same host the dashboard itself was loaded from (localhost, a LAN IP,
// whatever) on port 8080, since everyone runs their own full stack
// locally. A hardcoded IP here goes stale the moment DHCP reassigns one.
const API_BASE_URL = `http://${window.location.hostname}:8080`

export async function fetchLatestTransactions() {
  if (USE_MOCK) {
    // Simulate small network latency so loading/UI states can be tested too.
    await new Promise((resolve) => setTimeout(resolve, 150))
    return generateMockBatch(1, 3)
  }

  const res = await fetch(`${API_BASE_URL}/transactions`)
  if (!res.ok) {
    throw new Error(`Failed to fetch transactions: ${res.status}`)
  }
  return res.json()
}
