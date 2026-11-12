package ai.spatialreal.examples.pipecatclient

// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
object Config {
    const val APP_ID = "YOUR_APP_ID"

    /** The same avatar your bot's processor uses */
    const val AVATAR_ID = "YOUR_AVATAR_ID"

    /**
     * Pipecat's development runner from ../bot (`uv run bot.py -t daily --host 0.0.0.0`).
     * `POST /start` creates a Daily room and starts a bot in it.
     */
    const val BOT_SERVER_URL = "http://192.168.1.20:7860"

    /**
     * The token server from ../web-client (`npm run server`). It hands the app a SpatialReal
     * session token to download the avatar; your API key stays on the server.
     */
    const val TOKEN_SERVER_URL = "http://192.168.1.20:3000"

    // Use your computer's address on the network. From the Android emulator, 10.0.2.2 reaches
    // the computer it runs on: http://10.0.2.2:7860 and http://10.0.2.2:3000.
}
