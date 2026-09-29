"""A LiveKit Agents voice agent with a SpatialReal avatar.

The pipeline is Deepgram (speech to text), OpenAI (the replies) and Cartesia (text to speech).
The SpatialReal plugin takes the agent's speech: the avatar joins the room and speaks it.
"""

from dotenv import load_dotenv
from livekit.agents import Agent, AgentSession, JobContext, JobProcess, WorkerOptions, cli
from livekit.plugins import cartesia, deepgram, openai, silero, spatialreal

load_dotenv()


class Assistant(Agent):
    def __init__(self) -> None:
        super().__init__(
            instructions=(
                "You are a friendly voice assistant with a face. "
                "Your answers are spoken aloud, so keep them short and conversational."
            )
        )


def prewarm(proc: JobProcess):
    # Load voice activity detection once per worker process, not once per call
    proc.userdata["vad"] = silero.VAD.load()


async def entrypoint(ctx: JobContext):
    await ctx.connect()

    session = AgentSession(
        vad=ctx.proc.userdata["vad"],
        stt=deepgram.STT(),
        llm=openai.LLM(),
        tts=cartesia.TTS(),
        turn_detection="stt",  # Deepgram decides when the user has finished speaking
    )

    # Start the avatar before the agent session: from then on, whatever the agent says,
    # the avatar speaks in the room. Reads SPATIALREAL_API_KEY, _APP_ID and _AVATAR_ID.
    avatar = spatialreal.AvatarSession()
    await avatar.start(session, room=ctx.room)

    await session.start(agent=Assistant(), room=ctx.room)
    await session.generate_reply(instructions="Greet the user in one short sentence and offer your help.")


if __name__ == "__main__":
    # No agent_name: the worker joins every new room in the project
    cli.run_app(WorkerOptions(entrypoint_fnc=entrypoint, prewarm_fnc=prewarm))
