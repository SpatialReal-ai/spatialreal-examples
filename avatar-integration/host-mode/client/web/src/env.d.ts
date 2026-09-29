/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SPATIALREAL_APP_ID: string
  readonly VITE_SPATIALREAL_AVATAR_ID: string
  readonly VITE_HOST_SERVER_URL: string
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent
  export default component
}
