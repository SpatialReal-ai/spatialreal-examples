<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { SpatialReal, type AgoraAvatarSession, type SessionState } from '@spatialreal/web-sdk'

const container = ref<HTMLElement>()
const ag = shallowRef<AgoraAvatarSession>()
const state = ref<SessionState | 'loading'>('loading')
const muted = ref(false)
const error = ref('')
const channel = ref('')

// Agora clients whose token renewal is already wired: reconnect() creates a new one
const renewing = new WeakSet<object>()

// Create the session: it downloads the avatar and shows it, standing still.
// Joining the channel waits for start().
onMounted(async () => {
  try {
    const res = await fetch('/api/session', { method: 'POST' })
    const cfg = await res.json()
    if (!res.ok) throw new Error(cfg.error ?? `Token server answered ${res.status}`)
    channel.value = cfg.channel

    const sr = new SpatialReal({ appId: cfg.spatialrealAppId })
    const session = await sr.createSession({
      avatarId: cfg.avatarId,
      credential: cfg.credential,
      container: container.value!,
      // avatarUid is the uid your agent publishes the avatar with; uid is this page's
      agora: { appId: cfg.appId, channel: cfg.channel, token: cfg.token, avatarUid: cfg.avatarUid, uid: cfg.uid },
      // The microphone opens on start() and is published into the channel for your agent
      mic: 'required',
    })
    session.on('state', ({ current }) => {
      state.value = current
      if (current === 'live') renewTokenBeforeExpiry(session)
    })
    session.on('error', ({ error: e }) => (error.value = `${e.code}: ${e.message}`))
    // No motion arrived for a while during a reply: leave and join the channel again
    session.on('stalled', () => session.reconnect())
    state.value = session.state
    ag.value = session
  } catch (e) {
    error.value = describe(e)
  }
  window.addEventListener('pagehide', release)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', release)
  release()
})

// start() joins the channel and turns on the microphone. It must run from a click or tap
// so the browser lets the avatar's voice play.
async function start() {
  error.value = ''
  try {
    await ag.value?.start()
  } catch (e) {
    error.value = describe(e)
  }
}

// The SDK never renews the Agora token: fetch a fresh one when Agora asks for it
function renewTokenBeforeExpiry(session: AgoraAvatarSession) {
  const client = session.client
  if (!client || renewing.has(client)) return
  renewing.add(client)
  client.on('token-privilege-will-expire', async () => {
    try {
      const res = await fetch('/api/agora-token', { method: 'POST' })
      await client.renewToken((await res.json()).token)
    } catch (e) {
      error.value = `Token renewal failed: ${describe(e)}`
    }
  })
}

// Mute the microphone without leaving the channel
function toggleMute() {
  if (!ag.value) return
  ag.value.mic.muted = !ag.value.mic.muted
  muted.value = ag.value.mic.muted
}

// Leave the channel but keep the avatar on screen; Start joins again
function end() {
  ag.value?.end()
}

function release() {
  ag.value?.dispose()
}

function describe(e: unknown): string {
  if (e && typeof e === 'object' && 'code' in e) {
    const { code, message } = e as { code: string; message?: string }
    return `${code}: ${message}`
  }
  return e instanceof Error ? e.message : String(e)
}
</script>

<template>
  <main>
    <h1>Agora web client</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!ag || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live'" @click="toggleMute">{{ muted ? 'Unmute' : 'Mute' }}</button>
      <button :disabled="state !== 'live'" @click="end">End</button>
    </div>

    <p class="status">Session: {{ state }}<template v-if="channel"> · channel {{ channel }}</template></p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>
