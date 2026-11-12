# Pipecat client (iOS)

A SwiftUI app that joins your Pipecat bot's Daily room, sends the user's microphone to the bot, and renders the avatar that speaks the bot's replies. The app sends no speech to SpatialReal and opens no connection of its own to it: the bot's voice and the animation both arrive through the call.

Docs: [Pipecat iOS client](https://docs.spatialreal.ai/avatar-integration/pipecat/ios-client)

This is one client of the Pipecat example. Run the bot in [`../bot`](../bot) first, on Daily, and the token server from [`../web-client`](../web-client).

## Before you start

- Xcode 16 or later
- An iPhone or iPad on iOS 16 or later (A11 chip or newer), or the simulator on a Mac with Apple silicon
- The bot from [`../bot`](../bot), running on Daily and reachable from the device: `uv run bot.py -t daily --host 0.0.0.0`. It needs `pipecat-spatialreal` 0.2.0 or later
- The token server from [`../web-client`](../web-client), running: `npm install && npm run server` there (it reads the API key from that folder's `.env`)
- From [SpatialReal Studio](https://app.spatialreal.ai): your **App ID** ([Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)) and the same **avatar ID** your bot uses

## Configure

Open `PipecatClientExample.xcodeproj`. Xcode fetches the SpatialReal SDK on its own: the `SpatialRealSDK` and `SpatialRealPipecat` products of `ios-sdk-release`, which bring Pipecat's iOS client for Daily with them.

Fill in `PipecatClientExample/Config.swift`:

| Value | What it is |
| --- | --- |
| `appId` | Your App ID |
| `avatarId` | The avatar your bot uses |
| `botServerURL` | Pipecat's development runner. On a device, your computer's address on the network, for example `http://192.168.1.20:7860` |
| `tokenServerURL` | The token server. On a device, for example `http://192.168.1.20:3000` |

`localhost` works on the simulator. On a device, pick your team under **Signing & Capabilities**. The first time, iOS asks to allow the app on your local network, and then the microphone.

## Run

Choose your device or a simulator, and press **Run** (⌘R).

## What you should see

1. The avatar appears, standing still: the app fetched a session token and downloaded the avatar.
2. Tap **Start** and allow the microphone. The runner starts a bot in a new Daily room, the app joins it, and the bot greets you. From the first pause in its speech, the avatar speaks, lips in sync; the status line says where the bot's voice comes from.
3. Speak. The bot hears you, and the avatar speaks its reply. Talk over it, and it stops.
4. **Leave** ends the conversation and keeps the avatar on screen; **Start** begins a new one with a fresh bot.

## How it works

```mermaid
sequenceDiagram
    participant App as Your app<br/>(PipecatModel.swift)
    participant Server as Token server<br/>(web-client/server.js)
    participant Bot as Your bot<br/>(bot.py)
    participant Room as Daily room
    participant SR as SpatialReal

    Note over App,SR: The app opens
    App->>Server: POST /api/session-token
    Server-->>App: A session token
    App->>SR: createSession() with the App ID and the session token
    SR-->>App: The avatar
    Note over App: Standing still

    Note over App,SR: Tap Start
    App->>Bot: POST /start (Pipecat's runner)
    Bot->>Room: A new room: the bot joins
    App->>Room: startBotAndConnect(): join, microphone on
    App->>Bot: The avatar session asks for the bot's voice with the animation

    loop Every reply
        App->>Room: Your voice
        Room->>Bot: Your voice
        Bot->>SR: The reply's speech
        SR-->>Bot: The avatar's animation
        Bot->>Room: The animation and the voice, as RTVI messages
        Room->>App: handleServerMessage(): the SDK plays the voice and moves the face in sync
    end
```

A phone app on Daily cannot tap the bot's audio track, so the avatar session asks the bot's processor to send each reply's audio along with the animation, and plays it itself. Until the first pause in the bot's speech, Daily plays the bot's track as usual; then the session switches that track off and the avatar takes over. Leave the bot's audio track to the session: don't mute it, unsubscribe it or play it yourself.

Every conversation gets a new `PipecatClient` on the same `DailyTransport`, which the avatar session was created with. Leave ends the avatar session before disconnecting the client, while the call can still switch the bot's audio track back on.

`NSMicrophoneUsageDescription` and `NSLocalNetworkUsageDescription` are set in the target's build settings, and `Info.plist` allows plain `http://` on your local network. A production app talks to its server over `https://` and needs neither.

## Something doesn't work?

- **The avatar stands still while the bot talks**: the status line stays on "voice from the call". The bot must run `pipecat-spatialreal` 0.2.0 or later, with the same avatar.
- **Start says it could not reach the bot**: the runner must be running with `-t daily --host 0.0.0.0`, and `botServerURL` must be reachable from the device.
- **The bot doesn't hear you**: on a device, close Device Hub screen sharing; it can [take over the microphone](https://developer.apple.com/documentation/xcode/interacting-with-your-app-in-device-hub#Handle-camera-and-microphone-access-conflicts-on-physical-devices).
