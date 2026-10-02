/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SPATIALREAL_APP_ID: string
  readonly VITE_SPATIALREAL_AGENT_ID: string
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent
  export default component
}
