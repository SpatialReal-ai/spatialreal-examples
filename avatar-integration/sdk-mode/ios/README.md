# SDK mode on iOS

A SwiftUI app where the avatar speaks an audio clip: the app sends the clip to SpatialReal, and the avatar speaks it with lip-sync and expressions.

Docs: [SDK mode on iOS](https://docs.spatialreal.ai/avatar-integration/sdk-mode/ios)

## Before you start

- Xcode 16 or later
- An iPhone or iPad on iOS 16 or later (A11 chip or newer), or the simulator
- From [SpatialReal Studio](https://app.spatialreal.ai): your **App ID** ([Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)), an **avatar ID** ([Avatar library](https://docs.spatialreal.ai/studio/public-avatar)) and a **temporary session token** ([Session tokens](https://docs.spatialreal.ai/overview/session-tokens#temporary-tokens-for-testing))

## Configure

Open `SDKModeExample.xcodeproj`. Xcode fetches the SpatialReal SDK on its own.

Fill in `SDKModeExample/Config.swift`:

| Value | What it is |
| --- | --- |
| `appId` | Your App ID |
| `avatarId` | The avatar to show |
| `sessionToken` | A temporary token from Studio. A real app fetches one from its own server |

To run on a device, also pick your team under **Signing & Capabilities**.

## Run

Choose a simulator or your device, and press **Run** (⌘R).

## What you should see

1. The avatar appears, standing still. Nothing is billed yet.
2. Tap **Start**. The session goes `live`, and billing starts.
3. Tap **Speak (female voice)** or **Speak (male voice)**, whichever suits your avatar. The avatar speaks the clip, lips in sync, and returns to idle when it ends.
4. Tap **Stop** while it speaks: it stops at once. **End** closes the session and keeps the avatar on screen.

## How it maps to the docs

| Docs step | Where it is |
| --- | --- |
| Install the SDK | The `SpatialRealSDK` package in the project |
| Add a place for the avatar | `AvatarModel.container`, shown by `AvatarContainer` in `ContentView.swift` |
| Create the session | `AvatarModel.create()` |
| Start the session | `AvatarModel.start()` |
| Make the avatar speak | `AvatarModel.speak(_:)`, with the clip read by `AudioClip.pcm16(_:)` |
| Stop and clean up | `AvatarModel.stop()`, `end()`, `release()` |

The clips in `SDKModeExample/` are 16 kHz mono speech: one female voice, one male voice.

## Something doesn't work?

See [Common problems](https://docs.spatialreal.ai/avatar-integration/sdk-mode/ios#common-problems). A `credential-expired` error means the temporary token has run out: create a new one in Studio.
