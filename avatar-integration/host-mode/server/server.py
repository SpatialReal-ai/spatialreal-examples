"""Host mode server.

When the app asks, the server sends a clip to SpatialReal and forwards two things to the app,
in order: the clip's audio, and the avatar's motion that SpatialReal returns for it.
"""

import asyncio
import base64
import json
import os
import wave
from datetime import datetime, timedelta, timezone
from pathlib import Path

import websockets
from dotenv import load_dotenv
from spatialreal import new_avatar_session

load_dotenv()

SAMPLE_RATE = 16000
# localhost by default; set HOST=0.0.0.0 so a phone on your network can connect
HOST = os.getenv("HOST") or "localhost"
PORT = 8765
CLIPS = {voice: Path(__file__).parent / "clips" / f"{voice}.wav" for voice in ("female", "male")}


def b64(data: bytes) -> str:
    return base64.b64encode(data).decode()


def read_pcm16(path: Path) -> bytes:
    """The raw 16 kHz mono PCM16 inside a WAV file."""
    with wave.open(str(path)) as w:
        if (w.getframerate(), w.getnchannels(), w.getsampwidth()) != (SAMPLE_RATE, 1, 2):
            raise ValueError(f"{path.name} must be {SAMPLE_RATE} Hz mono PCM16")
        return w.readframes(w.getnframes())


async def handle(ws):
    outbox = asyncio.Queue()    # everything for the app, in order
    live = False                # False after an interrupt, until the next utterance
    speaking = None

    # Called with every motion message SpatialReal returns: forward it unchanged.
    # After the utterance's last one, tell the app the utterance is over; motion that
    # reaches the app after the end of an utterance is dropped.
    def on_motion(frame: bytes, is_last: bool):
        if live:
            outbox.put_nowait({"type": "motion", "data": [b64(frame)]})
            if is_last:
                outbox.put_nowait({"type": "audio", "data": "", "end": True})

    session = new_avatar_session(
        api_key=os.environ["SPATIALREAL_API_KEY"],
        app_id=os.environ["SPATIALREAL_APP_ID"],
        avatar_id=os.environ["SPATIALREAL_AVATAR_ID"],
        console_endpoint_url="https://api.spatialreal.com",
        ingress_endpoint_url="wss://driven.us-west.spatialreal.cloud/v2/driveningress",
        expire_at=datetime.now(timezone.utc) + timedelta(hours=1),
        sample_rate=SAMPLE_RATE,
        transport_frames=on_motion,
        on_error=lambda err: print("SpatialReal error:", err),
    )
    await session.init()    # exchanges the API key for a session token
    await session.start()   # connects to SpatialReal
    print("App connected; SpatialReal session started")

    # Queue the audio for the app first, then send it to SpatialReal, so the app has the
    # utterance open before its motion arrives. The audio to the app carries no end:
    # the end follows the last motion.
    async def speak(clip: Path):
        nonlocal live
        live = True
        pcm = read_pcm16(clip)
        outbox.put_nowait({"type": "audio", "data": b64(pcm), "end": False})
        await session.send_audio(pcm, end=True)

    async def pump():           # server -> app
        while True:
            await ws.send(json.dumps(await outbox.get()))

    async def listen():         # app -> server
        nonlocal live, speaking
        async for raw in ws:
            msg = json.loads(raw)
            if msg["type"] == "speak":
                clip = CLIPS.get(msg.get("voice", "female"))
                if clip:
                    speaking = asyncio.create_task(speak(clip))
            elif msg["type"] == "interrupt":
                # Stop producing speech, tell SpatialReal, and drop what the app hasn't received
                live = False
                if speaking:
                    speaking.cancel()
                await session.interrupt()
                while not outbox.empty():
                    outbox.get_nowait()

    try:
        await asyncio.gather(pump(), listen())
    finally:
        await session.close()
        print("App disconnected; SpatialReal session closed")


async def main():
    async with websockets.serve(handle, HOST, PORT):
        print(f"Host mode server on ws://{HOST}:{PORT}")
        await asyncio.Future()  # run until stopped


if __name__ == "__main__":
    asyncio.run(main())
