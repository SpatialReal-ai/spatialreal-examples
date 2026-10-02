<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { SpatialReal, type LiveKitAvatarSession, type SessionState } from '@spatialreal/web-sdk'

const container = ref<HTMLElement>()
const lk = shallowRef<LiveKitAvatarSession>()
const state = ref<SessionState | 'loading'>('loading')
const muted = ref(false)
const error = ref('')

// Create the session: it downloads the avatar and shows it, standing still.
// Joining the room waits for start().
onMounted(async () => {
  try {
    const res = await fetch('/api/tokens', { method: 'POST' })
    const tokens = await res.json()
    if (!res.ok) throw new Error(tokens.error ?? `Token server answered ${res.status}`)

    const sr = new SpatialReal({ appId: import.meta.env.VITE_SPATIALREAL_APP_ID })
    const session = await sr.createSession({
      avatarId: import.meta.env.VITE_SPATIALREAL_AVATAR_ID,
      credential: tokens.sessionToken,
      container: container.value!,
      livekit: { url: tokens.url, token: tokens.roomToken },
      mic: 'required',
    })
    session.on('state', ({ current }) => (state.value = current))
    session.on('error', ({ error: e }) => (error.value = `${e.code}: ${e.message}`))
    // No motion arrived for 3 s during a reply: rejoin the room
    session.on('stalled', () => session.reconnect())
    state.value = session.state
    lk.value = session
  } catch (e) {
    error.value = describe(e)
  }
  window.addEventListener('pagehide', release)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', release)
  release()
})

// start() joins the room and turns on the microphone. It must run from a click or tap
// so the browser lets the avatar's voice play.
async function start() {
  error.value = ''
  try {
    await lk.value?.start()
  } catch (e) {
    error.value = describe(e)
  }
}

// Mute the microphone without leaving the room
function toggleMute() {
  if (!lk.value) return
  lk.value.mic.muted = !lk.value.mic.muted
  muted.value = lk.value.mic.muted
}

// Leave the room but keep the avatar on screen; Start joins again
function leave() {
  lk.value?.end()
}

function release() {
  lk.value?.dispose()
}

function describe(e: unknown): string {
  if (e && typeof e === 'object' && 'code' in e) return `${(e as { code: string }).code}: ${(e as Error).message}`
  return e instanceof Error ? e.message : String(e)
}
</script>

<template>
  <main>
    <h1>LiveKit web client</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!lk || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live'" @click="toggleMute">{{ muted ? 'Unmute' : 'Mute' }}</button>
      <button :disabled="state !== 'live'" @click="leave">Leave</button>
    </div>

    <p class="status">Session: {{ state }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>
