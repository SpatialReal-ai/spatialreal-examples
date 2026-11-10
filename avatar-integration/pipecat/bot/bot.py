"""A Pipecat voice bot with a SpatialReal avatar.

Whatever the bot says, the avatar speaks it in the browser, with lip-sync and expressions.
Run it with Pipecat's development runner: ``uv run bot.py -t daily`` (or ``-t livekit``).
"""

import os

from dotenv import load_dotenv
from loguru import logger
from pipecat.audio.vad.silero import SileroVADAnalyzer
from pipecat.frames.frames import LLMRunFrame
from pipecat.pipeline.pipeline import Pipeline
from pipecat.pipeline.worker import PipelineParams, PipelineWorker
from pipecat.processors.aggregators.llm_context import LLMContext
from pipecat.processors.aggregators.llm_response_universal import (
    LLMContextAggregatorPair,
    LLMUserAggregatorParams,
)
from pipecat.runner.run import main
from pipecat.runner.types import RunnerArguments
from pipecat.runner.utils import create_transport
from pipecat.services.cartesia.tts import CartesiaTTSService
from pipecat.services.deepgram.stt import DeepgramSTTService
from pipecat.services.google.llm import GoogleLLMService
from pipecat.transports.daily.transport import DailyParams
from pipecat.transports.livekit.transport import LiveKitParams
from pipecat.workers.runner import WorkerRunner
from pipecat_spatialreal import SpatialRealAvatarProcessor

load_dotenv(override=True)

SYSTEM_INSTRUCTION = (
    "You are a friendly voice assistant. Your replies are spoken aloud: keep them to a few short "
    "sentences of plain text, with no lists, markdown or emoji."
)

transport_params = {
    "daily": lambda: DailyParams(audio_in_enabled=True, audio_out_enabled=True),
    "livekit": lambda: LiveKitParams(audio_in_enabled=True, audio_out_enabled=True),
}


async def bot(runner_args: RunnerArguments):
    transport = await create_transport(runner_args, transport_params)

    stt = DeepgramSTTService(api_key=os.environ["DEEPGRAM_API_KEY"])
    llm = GoogleLLMService(
        api_key=os.environ["GOOGLE_API_KEY"],
        settings=GoogleLLMService.Settings(system_instruction=SYSTEM_INSTRUCTION),
    )
    tts = CartesiaTTSService(
        api_key=os.environ["CARTESIA_API_KEY"],
        settings=CartesiaTTSService.Settings(
            voice="71a7ad14-091c-4e8e-a314-022ece01c121"
        ),
    )

    # The avatar: right after the TTS, before the transport's output
    avatar = SpatialRealAvatarProcessor(
        api_key=os.environ["SPATIALREAL_API_KEY"],
        app_id=os.environ["SPATIALREAL_APP_ID"],
        avatar_id=os.environ["SPATIALREAL_AVATAR_ID"],
    )

    context = LLMContext()
    user_aggregator, assistant_aggregator = LLMContextAggregatorPair(
        context, user_params=LLMUserAggregatorParams(vad_analyzer=SileroVADAnalyzer())
    )

    pipeline = Pipeline(
        [
            transport.input(),
            stt,
            user_aggregator,
            llm,
            tts,
            avatar,
            transport.output(),
            assistant_aggregator,
        ]
    )

    # RTVI is on by default: the animation reaches the page as RTVI server messages
    worker = PipelineWorker(pipeline, params=PipelineParams())

    # Greet once the page's Pipecat client is ready, so the first words are animated too
    @worker.rtvi.event_handler("on_client_ready")
    async def on_client_ready(rtvi):
        context.add_message(
            {
                "role": "developer",
                "content": "Say hello and briefly introduce yourself.",
            }
        )
        await worker.queue_frames([LLMRunFrame()])

    @transport.event_handler("on_client_disconnected")
    async def on_client_disconnected(transport, client):
        logger.info("Client left: ending the session")
        await worker.cancel()

    runner = WorkerRunner(handle_sigint=runner_args.handle_sigint)
    await runner.add_workers(worker)
    await runner.run()


if __name__ == "__main__":
    main()
