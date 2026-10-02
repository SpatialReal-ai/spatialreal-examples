# SpatialReal examples

Runnable examples for [SpatialReal](https://www.spatialreal.ai): photorealistic avatars that speak in real time, rendered on the viewer's device.

Every folder is a standalone project. Pick one, follow its README, and it runs on its own.

## Examples

### SpatialReal Agent

A conversational avatar you configure in [SpatialReal Studio](https://app.spatialreal.ai), with no code of its own.

| Example | What it shows | Status | Docs |
| --- | --- | --- | --- |
| [agent/embed](agent/embed) | Talk to an agent on your own page, with one `<iframe>` | Ready | [Quickstart](https://docs.spatialreal.ai/overview/quickstart) |
| [agent/web](agent/web) | Your own interface for an agent, with the Web SDK | Ready | [Your own app](https://docs.spatialreal.ai/agent/reach/your-own-app) |
| [agent/ios](agent/ios) | Your own interface for an agent, in an iOS app | Ready | [Your own app](https://docs.spatialreal.ai/agent/reach/your-own-app) |
| [agent/android](agent/android) | Your own interface for an agent, in an Android app | Ready | [Your own app](https://docs.spatialreal.ai/agent/reach/your-own-app) |

### Avatar Integration

An avatar for speech you already produce — your TTS, speech model or voice agent.

| Example | What it shows | Status | Docs |
| --- | --- | --- | --- |
| [avatar-integration/sdk-mode/web](avatar-integration/sdk-mode/web) | SDK mode: a web page sends speech and the avatar speaks it | Ready | [SDK mode on the Web](https://docs.spatialreal.ai/avatar-integration/sdk-mode/web) |
| [avatar-integration/sdk-mode/ios](avatar-integration/sdk-mode/ios) | SDK mode in an iOS app | Ready | [SDK mode on iOS](https://docs.spatialreal.ai/avatar-integration/sdk-mode/ios) |
| [avatar-integration/sdk-mode/android](avatar-integration/sdk-mode/android) | SDK mode in an Android app | Ready | [SDK mode on Android](https://docs.spatialreal.ai/avatar-integration/sdk-mode/android) |
| [avatar-integration/livekit/agent](avatar-integration/livekit/agent) | LiveKit: a LiveKit Agents voice agent with the SpatialReal plugin | Ready | [LiveKit Agent](https://docs.spatialreal.ai/avatar-integration/livekit/agent) |
| [avatar-integration/livekit/web-client](avatar-integration/livekit/web-client) | LiveKit: a web client that joins the room and renders the avatar | Ready | [Web client](https://docs.spatialreal.ai/avatar-integration/livekit/web-client) |
| [avatar-integration/host-mode/server](avatar-integration/host-mode/server) | Host mode: a server that forwards speech and motion to the app | Ready | [Host mode server](https://docs.spatialreal.ai/avatar-integration/host-mode/server) |
| [avatar-integration/host-mode/client/web](avatar-integration/host-mode/client/web) | Host mode: a web app that plays what the server forwards | Ready | [Host mode client](https://docs.spatialreal.ai/avatar-integration/host-mode/client) |
| [avatar-integration/host-mode/client/ios](avatar-integration/host-mode/client/ios) | Host mode: the same, in an iOS app | Ready | [Host mode client](https://docs.spatialreal.ai/avatar-integration/host-mode/client) |
| [avatar-integration/host-mode/client/android](avatar-integration/host-mode/client/android) | Host mode: the same, in an Android app | Ready | [Host mode client](https://docs.spatialreal.ai/avatar-integration/host-mode/client) |

Not sure which one you need? [How it works](https://docs.spatialreal.ai/overview/how-it-works) explains the two products, and [Avatar Integration](https://docs.spatialreal.ai/avatar-integration/introduction) helps you choose a mode.

## Before you start

You need a [SpatialReal Studio](https://app.spatialreal.ai) account. Depending on the example, you also need:

- an **App ID** and **API key** — [Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)
- an **avatar ID** — [Avatar library](https://docs.spatialreal.ai/studio/public-avatar)
- an **agent** — [SpatialReal Agent](https://docs.spatialreal.ai/agent/introduction)

Each example lists exactly what it needs.

## How the examples are laid out

- One folder, one runnable project. Nothing is shared between folders.
- Each has a README, a `.env.example` for your values, and at most three commands to run it.
- Web examples are Vite + Vue apps with a small Express server for tokens; they need Node.js 20.19 or later. Python examples use [uv](https://docs.astral.sh/uv/).

## License

[MIT](LICENSE)
