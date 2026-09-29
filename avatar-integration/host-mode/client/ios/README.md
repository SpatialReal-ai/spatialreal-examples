# Host mode client (iOS)

A SwiftUI app that connects to your Host mode server and plays the audio and motion it forwards. The avatar speaks with lip-sync and expressions; the app itself opens no connection to SpatialReal.

Docs: [Host mode client](https://docs.spatialreal.ai/avatar-integration/host-mode/client) (iOS tab)

This is an app side of the Host mode example. Start the server in [`../../server`](../../server) first.

## Before you start

- Xcode 16 or later
- An iPhone or iPad on iOS 16 or later (A11 chip or newer), or the simulator
- The Host mode server from [`../../server`](../../server), running
- From [SpatialReal Studio](https://app.spatialreal.ai): your **App ID** ([Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)), the same **avatar ID** the server uses, and a **temporary session token** ([Session tokens](https://docs.spatialreal.ai/overview/session-tokens#temporary-tokens-for-testing))

## Configure

Open `HostClientExample.xcodeproj`. Xcode fetches the SpatialReal SDK on its own.

Fill in `HostClientExample/Config.swift`:

| Value | What it is |
| --- | --- |
| `appId` | Your App ID |
| `avatarId` | The avatar the server uses |
| `sessionToken` | A temporary token from Studio, used only to download the avatar |
| `hostServerURL` | Your Host mode server. `ws://localhost:8765` works on the simulator |

On a device, set `hostServerURL` to your computer's address on the network (for example `ws://192.168.1.20:8765`), start the server with `HOST=0.0.0.0`, and pick your team under **Signing & Capabilities**. The first time, iOS asks to allow the app on your local network.

## Run

Choose a simulator or your device, and press **Run** (⌘R).

## What you should see

1. The avatar appears, standing still, and the app shows **Server: connected**.
2. Tap **Start**.
3. Tap **Speak (female voice)** or **Speak (male voice)**, whichever suits your avatar. The server sends the clip to SpatialReal and forwards the audio and motion; the avatar speaks it, lips in sync.
4. Tap **Stop** while it speaks: it stops at once, and the server stops forwarding.

The messages are the same as in the [web client](../web#messages).

## How it maps to the docs

| Docs step (iOS tab) | Where it is |
| --- | --- |
| Install the SDK | The `SpatialRealSDK` package in the project |
| Create a Host mode session | `HostModel.create()` |
| Hand over what your server sends | `HostModel.connect()`, `receive(from:)` |
| Start, then ask for speech | `HostModel.start()`, `speak(_:)` |
| Interrupt and clean up | `HostModel.stop()`, `release()` |

`Info.plist` lets the app reach a server on your own network over plain `ws://`. A production app talks to its server over `wss://`.

## Something doesn't work?

See [Common problems](https://docs.spatialreal.ai/avatar-integration/host-mode/client#common-problems). If the app says **Server: not connected**, check that the server is running and that `hostServerURL` points to it.
