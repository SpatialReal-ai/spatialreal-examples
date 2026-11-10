// Hands the page what it needs to join the Agora channel and render the avatar: an Agora
// RTC token for the page's uid, and a SpatialReal session token to download the avatar.
// Neither the SpatialReal API key nor the Agora App Certificate reaches the browser.
import express from 'express'
import agoraToken from 'agora-token'

const { RtcTokenBuilder, RtcRole } = agoraToken

const TOKEN_TTL_S = 3600

const config = {
  appId: process.env.AGORA_APP_ID,
  channel: process.env.AGORA_CHANNEL,
  avatarUid: Number(process.env.AVATAR_UID || 1001),
  uid: Number(process.env.USER_UID || 2001),
}

const app = express()

app.post('/api/session', async (req, res) => {
  try {
    res.json({
      ...config,
      token: createRtcToken(),
      credential: await createSessionToken(),
      spatialrealAppId: process.env.SPATIALREAL_APP_ID,
      avatarId: process.env.SPATIALREAL_AVATAR_ID,
    })
  } catch (err) {
    console.error(err.message)
    res.status(502).json({ error: err.message })
  }
})

// A fresh RTC token for the same uid and channel, before the current one expires
app.post('/api/agora-token', (req, res) => {
  res.json({ token: createRtcToken() })
})

app.listen(3000, () => console.log('Token server on http://localhost:3000'))

function createRtcToken() {
  // A project without a certificate takes the App ID itself as the token
  if (!process.env.AGORA_APP_CERTIFICATE) return config.appId
  // Publisher: the page publishes its microphone into the channel for the agent
  return RtcTokenBuilder.buildTokenWithUid(
    config.appId,
    process.env.AGORA_APP_CERTIFICATE,
    config.channel,
    config.uid,
    RtcRole.PUBLISHER,
    TOKEN_TTL_S,
    TOKEN_TTL_S,
  )
}

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
