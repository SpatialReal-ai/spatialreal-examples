# TEN Framework agent

A [TEN Framework](https://github.com/TEN-framework/ten-framework) voice agent with the SpatialReal avatar extension, `spatialreal_avatar_python`: whatever the agent says, the avatar speaks in your Agora channel, with lip-sync and expressions.

Docs: [TEN Framework agent](https://docs.spatialreal.ai/avatar-integration/ten-framework/agent)

This is one half of the TEN Framework example. The other half, the page users talk from, is in [`../web-client`](../web-client). Without TEN, the [Python SDK](#without-ten-python-sdk) publishes the avatar the same way.

## How the avatar reaches the channel

The extension takes the speech your TTS node produces and streams it to SpatialReal. SpatialReal joins the channel as its own uid (`params.agora_uid`) and publishes the agent's speech as an audio track, plus a placeholder video track whose frames carry the avatar's facial animation. There are no rendered video frames: the avatar is rendered by the viewer, with `@spatialreal/web-sdk` in Agora mode. The TEN playground and other plain Agora players show the audio and a black picture; use [`../web-client`](../web-client) to see the avatar.

Because the agent's speech reaches the channel through SpatialReal, the TTS audio goes to the avatar node instead of to `agora_rtc`.

## Before you start

- [Docker](https://docs.docker.com/get-docker/), and a checkout of TEN Framework with the extension (see [Get the extension](#get-the-extension))
- An [Agora](https://console.agora.io) project: its App ID and App Certificate, the same project the web client uses
- From [SpatialReal Studio](https://app.spatialreal.ai): your **API key**, **App ID** ([Apps & API keys](https://docs.spatialreal.ai/studio/api-keys)) and an **avatar ID** ([Avatar library](https://docs.spatialreal.ai/studio/public-avatar))
- API keys for the voice pipeline of the `voice_assistant_spatialreal` graph: [Deepgram](https://deepgram.com) (speech to text), [Groq](https://console.groq.com) (the replies) and [Cartesia](https://cartesia.ai) (text to speech)

## Get the extension

The extension is proposed to TEN Framework in [TEN-framework/ten-framework#2387](https://github.com/TEN-framework/ten-framework/pull/2387).

- **Once that PR is merged**, use TEN Framework's own repository:

  ```bash
  git clone https://github.com/TEN-framework/ten-framework.git
  ```

- **Until then**, use the branch the PR is opened from:

  ```bash
  git clone -b feat/spatialreal-avatar https://github.com/SpatialReal-ai/ten-framework.git
  ```

Either way, the extension is `ai_agents/agents/ten_packages/extension/spatialreal_avatar_python`, and its README is the reference for every property. The example app `ai_agents/agents/examples/voice-assistant-advanced` already has a graph with the avatar wired in: `voice_assistant_spatialreal`.

## Configure

TEN reads one file, `ai_agents/.env`:

```bash
cd ten-framework/ai_agents
cp .env.example .env
```

Add the variables in this folder's [`.env.example`](.env.example) to `ai_agents/.env`:

| Variable | What it is |
| --- | --- |
| `SPATIALREAL_API_KEY` | Your SpatialReal API key |
| `SPATIALREAL_AVATAR_ID` | The avatar that speaks for the agent. Use the same one in the web client |
| `SPATIALREAL_APP_ID` | Your SpatialReal App ID (optional for the extension) |
| `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE` | Your Agora project. The extension mints the avatar uid's token from them |
| `DEEPGRAM_API_KEY`, `GROQ_API_KEY`, `CARTESIA_TTS_KEY` | The voice pipeline of `voice_assistant_spatialreal` |

## Run

Start TEN's development container and its API server (port 8080):

```bash
docker compose up -d
docker exec -it ten_agent_dev bash
# inside the container
cd agents/examples/voice-assistant-advanced
task install
task run
```

Start the agent in a channel. The TEN API server fills `channel_name` into every graph node that has a `channel` property, the avatar node included:

```bash
curl http://localhost:8080/start \
  -H 'Content-Type: application/json' \
  -d '{
    "request_id": "spatialreal-demo-1",
    "channel_name": "spatialreal-demo",
    "user_uid": 2001,
    "graph_name": "voice_assistant_spatialreal",
    "timeout": 600,
    "properties": {
      "avatar": { "params": { "agora_uid": "1001" } }
    }
  }'
```

- `channel_name` is the web client's `AGORA_CHANNEL`.
- `user_uid` is the web client's `USER_UID`: the agent listens to this uid's microphone.
- `properties.avatar.params.agora_uid` is the avatar's uid, the web client's `AVATAR_UID`. The graph in TEN Framework sets `12345`; override it here, or edit the graph, so the two agree.
- `timeout` stops the agent after that many seconds without a `POST /ping`. The avatar holds a SpatialReal session for as long as the agent runs, so stop it when you're done:

  ```bash
  curl http://localhost:8080/stop -H 'Content-Type: application/json' \
    -d '{"request_id": "spatialreal-demo-1", "channel_name": "spatialreal-demo"}'
  ```

Now start the [web client](../web-client) with the same channel, uids, Agora project and avatar, click **Start** and speak. Use headphones: on speakers, the agent can hear its own voice and interrupt itself.

## What you should see

- When the agent starts, the avatar joins the channel as uid `1001`, publishing an audio track and a video track.
- In the web client, the agent greets you and, whatever it says, the avatar speaks, lips in sync.
- When you interrupt the agent, the avatar stops speaking too.

## Add the avatar to your own graph

[`graph-avatar-node.json`](graph-avatar-node.json) holds the avatar node and its three connections, the same as `voice_assistant_spatialreal` (with `agora_uid` set to the web client's default, `1001`):

1. Add the `avatar` node to your graph's `nodes`. Keep `"channel": ""` in its `property`: the TEN API server only fills in the channel for nodes that already have the key.
2. Merge the connections into your graph's existing `main_control` and `tts` entries:
   - `main_control` → `flush` → `avatar`, so an interruption stops the avatar;
   - `tts` → `pcm_frame` → `avatar`, the agent's speech;
   - `tts` → `tts_audio_end` → `avatar`, the end of each reply.
3. Remove the `pcm_frame` destination `agora_rtc` from your `tts` entry (and the matching `pcm_frame` source on `agora_rtc`, if your graph declares one). Otherwise the channel hears the agent twice: once from `agora_rtc`, once from the avatar.

A TTS that ends a reply with data `finalize` instead of `tts_audio_end` works too: route `finalize` to `avatar` the same way.

The `params` worth knowing:

| Param | What it does |
| --- | --- |
| `agora_uid` | The avatar's uid in the channel, a non-zero integer written as a string. **Required** |
| `agora_appid`, `agora_appcert` | Your Agora project. The extension mints a token for `agora_uid` on every connect |
| `agora_token` | A token you minted yourself, used as given instead |
| `sample_rate` | The SpatialReal session rate, `24000` by default. TTS audio at another rate is resampled |
| `source_timeline_only` | `true` by default. Set `false` for compatibility mode (below) |
| `environment`, `console_endpoint_url`, `ingress_endpoint_url` | Which SpatialReal deployment to use. Leave the defaults |

### Compatibility mode

By default the avatar is published in source-timeline mode with bounded burst playback. Viewers need `@spatialreal/web-sdk` 1.0.0-beta.7 or later, which detects this by itself, and SpatialReal's backend must confirm both capabilities when the session starts.

For viewers that play the egress's native audio track (older SDKs), set `"source_timeline_only": false` in `params` and leave `source_playback_profile` out.

### Errors

The extension reports errors on data `error`, with `vendor_info.vendor` set to `spatialreal`:

| Code | Meaning | What to do |
| --- | --- | --- |
| `-1012` | Configuration: a missing or invalid param (the message names it), or the backend didn't confirm the playback capabilities | Fix the named param; for a capability error, use compatibility mode or a current backend. The extension stays idle |
| `-1` | Fatal: credits exhausted, the API key refused (`401` / `403`), or reconnecting gave up | Check the API key and your credits, then start the agent again |
| `1001` | Not fatal: the extension is reconnecting, or a concurrency slot was refused (close code `4003`) | Nothing; it retries by itself |

`vendor_info.code` carries the server's close code, the HTTP status of a failed token exchange, or the exception's class name.

## Without TEN: Python SDK

Any Python server can publish the avatar into a channel with the [SpatialReal Python SDK](https://pypi.org/project/spatialreal/): give the session an `AgoraEgressConfig`, then send it your speech as 16-bit mono PCM. This script makes the avatar speak a WAV file, which is enough to try the web client with no agent and no microphone:

```python
"""Publish a SpatialReal avatar into an Agora channel and make it speak a WAV file."""

import asyncio
import os
import time
import wave
from datetime import datetime, timedelta, timezone

from agora_token_builder import RtcTokenBuilder
from dotenv import load_dotenv
from spatialreal import AgoraEgressConfig, new_avatar_session

load_dotenv()
CHANNEL, AVATAR_UID = os.environ["AGORA_CHANNEL"], int(os.environ.get("AVATAR_UID", "1001"))


async def main(path: str) -> None:
    with wave.open(path) as w:  # 16-bit mono PCM, at 16000 or 24000 Hz for example
        rate, pcm = w.getframerate(), w.readframes(w.getnframes())
    # An RTC token for the avatar's uid, publisher role (1), valid for an hour
    token = RtcTokenBuilder.buildTokenWithUid(
        os.environ["AGORA_APP_ID"], os.environ["AGORA_APP_CERTIFICATE"], CHANNEL, AVATAR_UID, 1, int(time.time()) + 3600
    )
    session = new_avatar_session(
        api_key=os.environ["SPATIALREAL_API_KEY"],
        app_id=os.environ.get("SPATIALREAL_APP_ID", ""),
        avatar_id=os.environ["SPATIALREAL_AVATAR_ID"],
        expire_at=datetime.now(timezone.utc) + timedelta(hours=1),
        sample_rate=rate,
        agora_egress=AgoraEgressConfig(channel_name=CHANNEL, token=token, uid=AVATAR_UID),
    )
    await session.init()  # the API key buys a session token
    await session.start()  # the avatar joins the channel as AVATAR_UID
    chunk = rate * 2 // 50  # 20 ms of audio
    for i in range(0, len(pcm), chunk):
        await session.send_audio(pcm[i : i + chunk])
        await asyncio.sleep(0.01)  # faster than real time, like a TTS stream
    await session.send_audio(b"", end=True)  # the end of this reply
    await asyncio.sleep(len(pcm) / (rate * 2) + 3)  # let it finish playing
    await session.close()


asyncio.run(main("speech.wav"))
```

Save it as `publish_avatar.py` next to a `speech.wav`, fill in a `.env` from [`.env.example`](.env.example) (the SpatialReal and Agora values, `AGORA_CHANNEL` and `AVATAR_UID`), click **Start** in the web client, then run:

```bash
uv run --with spatialreal==0.4.0 --with agora-token-builder --with python-dotenv publish_avatar.py
```

In your own agent, call `send_audio()` with each chunk of your TTS output and `send_audio(b"", end=True)` at the end of each reply; `session.interrupt()` stops the current reply. As with the extension, `spatialreal` 0.4.0 publishes in source-timeline mode with bounded burst playback by default; `AgoraEgressConfig(..., source_timeline_only=False)` is compatibility mode. A server that doesn't confirm the playback capabilities makes `start()` raise `UnsupportedConfigurationError`.

## Something doesn't work?

- **The avatar never joins the channel**: read the agent's log for the avatar's error (see [Errors](#errors)). Check the `SPATIALREAL_*` and `AGORA_*` values in `ai_agents/.env`, and restart the container after editing it.
- **The avatar joins but the web client shows nothing**: the web client's `AVATAR_UID` must equal `agora_uid`, and both sides must use the same Agora project and channel.
- **The agent never answers**: `user_uid` in `/start` must equal the web client's `USER_UID`.
- **Black video**: that is a plain Agora player, the TEN playground included. Only `@spatialreal/web-sdk` in Agora mode renders the avatar.
- **The agent's voice plays twice**: the TTS still sends `pcm_frame` to `agora_rtc`; remove that connection.
