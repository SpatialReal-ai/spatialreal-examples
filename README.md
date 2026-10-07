<div align="center">

<img src=".github/banner.png" alt="SpatialReal Examples" width="100%">

Runnable examples for [SpatialReal](https://www.spatialreal.ai): photorealistic avatars that speak in real time, rendered on the viewer's device.

[![Docs](https://img.shields.io/badge/docs-docs.spatialreal.ai-df69b6)](https://docs.spatialreal.ai)
[![Studio](https://img.shields.io/badge/Studio-app.spatialreal.ai-df69b6)](https://app.spatialreal.ai)
[![Discord](https://img.shields.io/badge/Discord-join%20us-5865F2?logo=discord&logoColor=white)](https://discord.gg/TfKman55T2)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-follow-0A66C2)](https://www.linkedin.com/company/spatialreal)

[![Web SDK on npm](https://img.shields.io/badge/Web%20SDK-npm-CB3837?logo=npm&logoColor=white)](https://www.npmjs.com/package/@spatialreal/web-sdk)
[![iOS SDK, Swift Package Manager](https://img.shields.io/badge/iOS%20SDK-Swift%20Package-F05138?logo=swift&logoColor=white)](https://github.com/SpatialReal-ai/ios-sdk-release)
[![Android SDK on Maven Central](https://img.shields.io/badge/Android%20SDK-Maven%20Central-3DDC84?logo=android&logoColor=white)](https://central.sonatype.com/artifact/ai.spatialreal/android)
[![Python SDK on PyPI](https://img.shields.io/badge/Python%20SDK-PyPI-3775A9?logo=pypi&logoColor=white)](https://pypi.org/project/spatialreal/)
[![LiveKit plugin on PyPI](https://img.shields.io/badge/LiveKit%20plugin-PyPI-3775A9?logo=pypi&logoColor=white)](https://pypi.org/project/livekit-plugins-spatialreal/)

</div>

## Start here

- **See an avatar talk, with no code:** [`agent/embed`](agent/embed). Create an agent in Studio, paste its link into one `<iframe>`, open the page and speak.
- **Make an avatar speak your own audio:** [`avatar-integration/sdk-mode/web`](avatar-integration/sdk-mode/web). A web page sends an audio clip, and the avatar speaks it with lip-sync and expressions.

Every folder is a standalone project. Pick one, follow its README, and it runs on its own.

## Examples

### SpatialReal Agent

A conversational avatar you configure in [SpatialReal Studio](https://app.spatialreal.ai). SpatialReal runs the whole conversation.

| | Web | iOS | Android |
| --- | --- | --- | --- |
| **[On a page](https://docs.spatialreal.ai/agent/reach/page-and-embed)**<br>One `<iframe>`, no code | [embed](agent/embed) | — | — |
| **[In your own app](https://docs.spatialreal.ai/agent/reach/your-own-app)**<br>Your interface; the SDK runs the conversation | [web](agent/web) | [ios](agent/ios) | [android](agent/android) |

### Avatar Integration

An avatar for speech you already produce: your TTS, speech model or voice agent.

| | Web | iOS | Android | Server |
| --- | --- | --- | --- | --- |
| **[SDK mode](https://docs.spatialreal.ai/avatar-integration/sdk-mode/overview)**<br>Your app sends the speech; the avatar speaks it | [web](avatar-integration/sdk-mode/web) | [ios](avatar-integration/sdk-mode/ios) | [android](avatar-integration/sdk-mode/android) | — |
| **[LiveKit](https://docs.spatialreal.ai/avatar-integration/livekit/overview)**<br>The avatar joins your LiveKit voice agent's room | [web-client](avatar-integration/livekit/web-client) | — | — | [agent](avatar-integration/livekit/agent) (Python) |
| **[Host mode](https://docs.spatialreal.ai/avatar-integration/host-mode/overview)**<br>Your server forwards speech and motion to your app | [client/web](avatar-integration/host-mode/client/web) | [client/ios](avatar-integration/host-mode/client/ios) | [client/android](avatar-integration/host-mode/client/android) | [server](avatar-integration/host-mode/server) (Python) |

LiveKit and Host mode each come in two halves: start the server side first, then a client.

Not sure which one you need? [How it works](https://docs.spatialreal.ai/overview/how-it-works) explains the two products, and [Avatar Integration](https://docs.spatialreal.ai/avatar-integration/introduction) helps you choose a mode.

## Before you start

You need a [SpatialReal Studio](https://app.spatialreal.ai) account. Depending on the example, you also need:

- an **App ID** and **API key**: [Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)
- an **avatar ID**: [Avatar library](https://docs.spatialreal.ai/studio/public-avatar)
- an **agent**: [SpatialReal Agent](https://docs.spatialreal.ai/agent/introduction)

Each example lists exactly what it needs.

## How the examples are laid out

- One folder, one runnable project. Nothing is shared between folders.
- Each has a README with at most three commands to run it, and a place for your values: `.env.example`, or a config file in the mobile apps.
- Each README links its docs page, and maps the code to the page's steps so you can read them side by side.
- Web examples are Vite + Vue apps with a small Express server for tokens; they need Node.js 20.19 or later. Python examples use [uv](https://docs.astral.sh/uv/).

## Community

- Questions, feedback, or something you built: join us on [Discord](https://discord.gg/TfKman55T2).
- Something wrong in an example: [open an issue](https://github.com/SpatialReal-ai/spatialreal-examples/issues).
- News and releases: follow [SpatialReal on LinkedIn](https://www.linkedin.com/company/spatialreal).

## License

[MIT](LICENSE)
