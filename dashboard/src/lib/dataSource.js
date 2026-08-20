// Single point of contact between the UI and "wherever transactions come
// from." Right now that's the mock generator. Once Alex's endpoint is
// live, this is the only file that needs to change - every component
// keeps calling fetchLatestTransactions() exactly the same way.

import { generateMockBatch } from './mockData'

const USE_MOCK = false // flip to false once API_BASE_URL is real

const API_BASE_URL = 'http://10.0.0.131:8080'

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
