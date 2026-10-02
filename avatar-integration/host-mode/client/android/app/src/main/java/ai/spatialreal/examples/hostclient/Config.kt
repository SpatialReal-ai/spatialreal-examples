package ai.spatialreal.examples.hostclient

// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
object Config {
    const val APP_ID = "YOUR_APP_ID"

    /** The same avatar your Host mode server uses */
    const val AVATAR_ID = "YOUR_AVATAR_ID"

    /**
     * A session token, used only to download the avatar. For a first try, create a temporary
     * token in Studio and paste it here. A real app fetches one from its own server instead:
     * https://docs.spatialreal.ai/overview/session-tokens
     */
    const val SESSION_TOKEN = "YOUR_SESSION_TOKEN"

    /**
     * Your Host mode server: your computer's address on the network, for example
     * ws://192.168.1.20:8765, with the server started with HOST=0.0.0.0.
     * From the Android emulator, ws://10.0.2.2:8765 reaches the computer it runs on.
     */
    const val HOST_SERVER_URL = "ws://192.168.1.20:8765"
}
