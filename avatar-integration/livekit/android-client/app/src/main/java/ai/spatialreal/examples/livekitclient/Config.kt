package ai.spatialreal.examples.livekitclient

// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
object Config {
    const val APP_ID = "YOUR_APP_ID"

    /** The same avatar your agent uses */
    const val AVATAR_ID = "YOUR_AVATAR_ID"

    /**
     * The token server from ../web-client (`npm run server`). It hands the app a LiveKit room
     * token and a SpatialReal session token; your API keys stay on the server.
     * Use your computer's address on the network, for example http://192.168.1.20:3000.
     * From the Android emulator, http://10.0.2.2:3000 reaches the computer it runs on.
     */
    const val TOKEN_SERVER_URL = "http://192.168.1.20:3000"
}
