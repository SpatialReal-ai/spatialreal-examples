// Issues SpatialReal session tokens, so the API key never reaches the browser.
import express from 'express'

const app = express()

app.post('/api/session-token', async (req, res) => {
  try {
    res.json({ token: await createSessionToken() })
  } catch (err) {
    console.error(err.message)
    res.status(502).json({ error: err.message })
  }
})

app.listen(3000, () => console.log('Token server on http://localhost:3000'))

async function createSessionToken() {
  const r = await fetch('https://api.spatialreal.com/v1/auth/session-token', {
    method: 'POST',
    headers: {
      'X-API-KEY': process.env.SPATIALREAL_API_KEY,
      'Content-Type': 'application/json',
    },
    // unix seconds, at most 24 hours ahead
    body: JSON.stringify({ expire_at: Math.floor(Date.now() / 1000) + 3600 }),
  })
  if (!r.ok) throw new Error(`Session token request failed: ${r.status} ${await r.text()}`)
  return (await r.json()).session_token
}
