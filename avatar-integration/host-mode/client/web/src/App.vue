<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { SpatialReal, type HostAvatarSession, type SessionState } from '@spatialreal/web-sdk'

// The server forwards 16 kHz mono PCM16; the session is told the same rate
const SAMPLE_RATE = 16000

const container = ref<HTMLElement>()
const host = shallowRef<HostAvatarSession>()
const state = ref<SessionState | 'loading'>('loading')
const serverConnected = ref(false)
const error = ref('')
let ws: WebSocket | undefined

// base64 text back to bytes
const bytes = (b64: string) => Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))

onMounted(async () => {
  try {
    // Create a Host mode session: the token is used only to download the avatar.
    // The SDK opens no connection of its own; everything arrives through your server.
    const sr = new SpatialReal({ appId: import.meta.env.VITE_SPATIALREAL_APP_ID })
    const session = await sr.createSession({
      avatarId: import.meta.env.VITE_SPATIALREAL_AVATAR_ID,
      credential: await fetchSessionToken(),
      container: container.value!,
      drivingServiceMode: 'host',
      audioFormat: { sampleRate: SAMPLE_RATE },
    })
    session.on('state', ({ current }) => (state.value = current))
    session.on('error', ({ error: e }) => (error.value = `${e.code}: ${e.message}`))
    state.value = session.state
    host.value = session
    connect(session)
  } catch (e) {
    error.value = describe(e)
  }
  window.addEventListener('pagehide', release)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', release)
  release()
})

// Hand over everything the server sends, as it arrives: audio to yieldAudioData(), motion to yieldFramesData()
function connect(session: HostAvatarSession) {
  ws = new WebSocket(import.meta.env.VITE_HOST_SERVER_URL)
  ws.onopen = () => (serverConnected.value = true)
  ws.onclose = () => (serverConnected.value = false)
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)
    if (msg.type === 'audio') session.yieldAudioData(bytes(msg.data).buffer, msg.end)
    if (msg.type === 'motion') session.yieldFramesData(msg.data.map(bytes))
  }
}

// start() opens no connection in Host mode; it unlocks audio, so it must run from a click or tap
async function start() {
  error.value = ''
  try {
    await host.value?.start()
  } catch (e) {
    error.value = describe(e)
  }
}

// Ask the server for speech
function speak(voice: 'female' | 'male') {
  ws?.send(JSON.stringify({ type: 'speak', voice }))
}

// Stop playback here and tell the server, so it stops forwarding the old utterance
function stop() {
  host.value?.interrupt()
  ws?.send(JSON.stringify({ type: 'interrupt' }))
}

function release() {
  ws?.close()
  host.value?.dispose()
}

async function fetchSessionToken(): Promise<string> {
  const res = await fetch('/api/session-token', { method: 'POST' })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `Token server answered ${res.status}`)
  return body.token
}

function describe(e: unknown): string {
  if (e && typeof e === 'object' && 'code' in e) return `${(e as { code: string }).code}: ${(e as Error).message}`
  return e instanceof Error ? e.message : String(e)
}
</script>

<template>
  <main>
    <h1>Host mode client</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!host || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live' || !serverConnected" @click="speak('female')">Speak (female voice)</button>
      <button :disabled="state !== 'live' || !serverConnected" @click="speak('male')">Speak (male voice)</button>
      <button :disabled="state !== 'live'" @click="stop">Stop</button>
    </div>

    <p class="status">Session: {{ state }} · Server: {{ serverConnected ? 'connected' : 'not connected' }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>
