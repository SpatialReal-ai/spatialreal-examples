<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import {
  SessionEndedError,
  SpatialReal,
  type ChatSession,
  type SessionState,
  type TranscriptEvent,
} from '@spatialreal/web-sdk'

interface Line {
  key: string
  role: TranscriptEvent['role']
  text: string
}

const container = ref<HTMLElement>()
const chat = shallowRef<ChatSession>()
const state = ref<SessionState | 'loading'>('loading')
const lines = ref<Line[]>([])
const notice = ref('')

// Create the session: it checks the token and shows the agent's avatar.
// It doesn't connect yet, so nothing is billed.
onMounted(async () => {
  try {
    const sr = new SpatialReal({ appId: import.meta.env.VITE_SPATIALREAL_APP_ID })
    const session = await sr.createSession({
      agentId: import.meta.env.VITE_SPATIALREAL_AGENT_ID,
      credential: await fetchSessionToken(),
      container: container.value!,
    })
    session.on('transcript', showLine)
    session.on('state', ({ current }) => (state.value = current))
    session.on('error', ({ error }) => {
      // session-ended: the agent, a time limit or your credits ended the conversation.
      // Don't reconnect on your own; let the user start again.
      notice.value =
        error instanceof SessionEndedError ? `The conversation ended (${error.reason}).` : `${error.code}: ${error.message}`
    })
    state.value = session.state
    chat.value = session
  } catch (e) {
    notice.value = describe(e)
  }
  window.addEventListener('pagehide', release)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', release)
  release()
})

// start() must run from a click, a tap or a key press so the browser lets the agent's voice play.
// It also opens the microphone.
async function start() {
  notice.value = ''
  lines.value = []
  try {
    await chat.value?.start()
  } catch (e) {
    notice.value = describe(e)
  }
}

// end() finishes the conversation but keeps the session: Start opens a new one
function end() {
  chat.value?.end()
}

function release() {
  chat.value?.dispose()
}

// text is the whole utterance so far: replace this turn's line, don't append to it
function showLine({ role, text, turnId }: TranscriptEvent) {
  const key = `${turnId}:${role}`
  const line = lines.value.find((l) => l.key === key)
  if (line) line.text = text
  else lines.value.push({ key, role, text })
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
    <h1>Agent in your own interface</h1>
    <div ref="container" class="avatar"></div>

    <div class="controls">
      <button :disabled="!chat || state !== 'idle'" @click="start">Start</button>
      <button :disabled="state !== 'live'" @click="end">End</button>
      <span class="status">{{ state }}</span>
    </div>

    <p v-if="notice" class="error">{{ notice }}</p>

    <ul class="captions">
      <li v-for="line in lines" :key="line.key" :class="line.role">{{ line.text }}</li>
    </ul>
  </main>
</template>
