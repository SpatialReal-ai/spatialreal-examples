<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { SpatialReal, type PipecatAvatarSession, type SessionState } from '@spatialreal/web-sdk'
import { PipecatClient, type Participant } from '@pipecat-ai/client-js'
import { DailyTransport } from '@pipecat-ai/daily-transport'
import { LiveKitTransport } from '@pipecat-ai/livekit-transport'

// Must match the transport your bot runs with: uv run bot.py -t daily (or -t livekit)
const TRANSPORT = import.meta.env.VITE_PIPECAT_TRANSPORT === 'livekit' ? 'livekit' : 'daily'

const container = ref<HTMLElement>()
const avatar = shallowRef<PipecatAvatarSession>()
const client = shallowRef<PipecatClient>()
const state = ref<SessionState | 'loading'>('loading')
const botReady = ref(false)
const error = ref('')

// Create the session: it downloads the avatar and shows it, standing still.
// It opens no connection of its own: the bot's voice and animation arrive through Pipecat.
onMounted(async () => {
  try {
    const res = await fetch('/api/session-token', { method: 'POST' })
    const body = await res.json()
    if (!res.ok) throw new Error(body.error ?? `Token server answered ${res.status}`)

    const sr = new SpatialReal({ appId: import.meta.env.VITE_SPATIALREAL_APP_ID })
    const session = await sr.createSession({
      avatarId: import.meta.env.VITE_SPATIALREAL_AVATAR_ID,
      credential: body.sessionToken,
      container: container.value!,
      pipecat: {},
    })
    session.on('state', ({ current }) => (state.value = current))
    session.on('error', ({ error: e }) => (error.value = `${e.code}: ${e.message}`))
    // The avatar fell back to plain audio for a while, and why
    session.on('degraded', ({ reason }) => console.warn('avatar degraded:', reason))
    state.value = session.state
    avatar.value = session
  } catch (e) {
    error.value = describe(e)
  }
  window.addEventListener('pagehide', release)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', release)
  release()
})

// Start must run from a click so the browser lets the avatar's voice play:
// session.start() comes first, before anything else is awaited.
async function start() {
  const session = avatar.value
  if (!session) return
  error.value = ''
  try {
    await session.start()

    // Ask Pipecat's development runner for a bot session in a new room
    const res = await fetch('/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TRANSPORT === 'daily' ? { createDailyRoom: true } : { transport: 'livekit' }),
    })
    if (!res.ok) throw new Error(`/start answered ${res.status}: is the bot running with -t ${TRANSPORT}?`)
    const started = await res.json()
    const room =
      TRANSPORT === 'daily'
        ? { url: started.dailyRoom, token: started.dailyToken }
        : { url: started.url, token: started.token }
    const isBot = botParticipant(started)

    const pc = new PipecatClient({
      transport: TRANSPORT === 'daily' ? new DailyTransport() : new LiveKitTransport(),
      enableMic: true,
      callbacks: {
        // The SDK plays the bot's voice, in sync with the face. Never play the track yourself.
        onTrackStarted: (track, participant) => {
          if (track.kind === 'audio' && !participant?.local && isBot(participant)) session.setBotAudioTrack(track)
        },
        // Every RTVI server message goes to the SDK; it takes the avatar's and ignores the rest
        onServerMessage: (data) => session.handleServerMessage(data),
        onBotReady: () => (botReady.value = true),
        onDisconnected: () => leave(),
      },
    })
    client.value = pc
    await pc.connect(room)
  } catch (e) {
    error.value = describe(e)
    await leave()
  }
}

// Leave the room but keep the avatar on screen; Start begins a new conversation
async function leave() {
  const pc = client.value
  client.value = undefined
  botReady.value = false
  await pc?.disconnect().catch(() => {})
  if (avatar.value?.state === 'live') await avatar.value.end()
}

function release() {
  client.value?.disconnect()
  avatar.value?.dispose()
}

// Pipecat's development runner names the bot "Pipecat Bot" on Daily, and on LiveKit gives it
// the identity "Pipecat Agent-" plus the first 8 characters of the session id
function botParticipant(started: { sessionId?: string }) {
  if (TRANSPORT === 'daily') return (p?: Participant) => p?.name === 'Pipecat Bot'
  const identity = `Pipecat Agent-${String(started.sessionId).slice(0, 8)}`
  return (p?: Participant) => p?.id === identity
}

function describe(e: unknown): string {
  if (e instanceof Error && 'code' in e) return `${(e as Error & { code: string }).code}: ${e.message}`
  return e instanceof Error ? e.message : String(e)
}
</script>

<template>
  <main>
    <h1>Pipecat web client</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!avatar || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live'" @click="leave">Leave</button>
    </div>

    <p class="status">Session: {{ state }}{{ botReady ? ' · bot ready, say something' : '' }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>
