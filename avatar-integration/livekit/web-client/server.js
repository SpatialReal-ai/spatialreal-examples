// Hands the page two tokens: a LiveKit room token to join the room, and a SpatialReal
// session token to download the avatar. Neither API key reaches the browser.
import express from 'express'
import { AccessToken } from 'livekit-server-sdk'

const app = express()

app.post('/api/tokens', async (req, res) => {
  try {
    // A new room per page load: your agent joins it when the user does
    const room = `spatialreal-${crypto.randomUUID()}`
    const roomToken = new AccessToken(process.env.LIVEKIT_API_KEY, process.env.LIVEKIT_API_SECRET, {
      identity: `user-${crypto.randomUUID()}`,
    })
    roomToken.addGrant({ roomJoin: true, room })

    res.json({
      url: process.env.LIVEKIT_URL,
      roomToken: await roomToken.toJwt(),
      sessionToken: await createSessionToken(),
    })
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
