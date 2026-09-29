<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { SpatialReal, type AvatarSession, type SessionState } from '@spatialreal/web-sdk'

// The clips in public/ are 16 kHz mono; the session is told the same rate
const SAMPLE_RATE = 16000

const container = ref<HTMLElement>()
const avatar = shallowRef<AvatarSession>()
const state = ref<SessionState | 'loading'>('loading')
const error = ref('')

// Create the session: it checks the token, loads the avatar and shows it.
// It doesn't connect yet, so nothing is billed.
onMounted(async () => {
  try {
    const sr = new SpatialReal({ appId: import.meta.env.VITE_SPATIALREAL_APP_ID })
    const session = await sr.createSession({
      avatarId: import.meta.env.VITE_SPATIALREAL_AVATAR_ID,
      credential: await fetchSessionToken(),
      container: container.value!,
      audioFormat: { sampleRate: SAMPLE_RATE },
    })
    session.on('state', ({ current }) => (state.value = current))
    session.on('error', ({ error: e }) => (error.value = `${e.code}: ${e.message}`))
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

// Browsers only play sound after the user interacts with the page, so start from a click
async function start() {
  error.value = ''
  try {
    await avatar.value?.start()
  } catch (e) {
    error.value = describe(e)
  }
}

// Send the whole clip in one call; `true` marks the end of the utterance
async function speak(clip: string) {
  try {
    avatar.value?.send(await loadPcm16(clip), true)
  } catch (e) {
    error.value = describe(e)
  }
}

// The user talks over the avatar: stop speaking at once
function stop() {
  avatar.value?.interrupt()
}

function release() {
  avatar.value?.dispose()
}

async function fetchSessionToken(): Promise<string> {
  const res = await fetch('/api/session-token', { method: 'POST' })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `Token server answered ${res.status}`)
  return body.token
}

// Decode an audio file into mono PCM16 at the given sample rate
async function loadPcm16(url: string, sampleRate = SAMPLE_RATE): Promise<ArrayBuffer> {
  const file = await (await fetch(url)).arrayBuffer()
  const decoded = await new OfflineAudioContext(1, 1, sampleRate).decodeAudioData(file)
  const samples = decoded.getChannelData(0) // floats between -1 and 1
  const pcm = new Int16Array(samples.length)
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    pcm[i] = s < 0 ? s * 0x8000 : s * 0x7fff
  }
  return pcm.buffer
}

function describe(e: unknown): string {
  if (e && typeof e === 'object' && 'code' in e) return `${(e as { code: string }).code}: ${(e as Error).message}`
  return e instanceof Error ? e.message : String(e)
}
</script>

<template>
  <main>
    <h1>SDK mode on the Web</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!avatar || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live'" @click="speak('/female.wav')">Speak (female voice)</button>
      <button :disabled="state !== 'live'" @click="speak('/male.wav')">Speak (male voice)</button>
      <button :disabled="state !== 'live'" @click="stop">Stop</button>
    </div>

    <p class="status">Session: {{ state }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>
