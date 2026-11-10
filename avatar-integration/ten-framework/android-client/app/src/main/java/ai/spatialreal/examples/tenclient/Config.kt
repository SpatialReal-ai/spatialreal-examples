package ai.spatialreal.examples.tenclient

object Config {
    /**
     * The token server from ../web-client (`npm run server`). It hands the app everything else:
     * the Agora App ID, channel, uids and RTC token, and the SpatialReal App ID, avatar ID and
     * session token. Those values live in ../web-client/.env; your API key and your Agora
     * App Certificate stay on the server.
     * Use your computer's address on the network, for example http://192.168.1.20:3000.
     * From the Android emulator, http://10.0.2.2:3000 reaches the computer it runs on.
     */
    const val TOKEN_SERVER_URL = "http://192.168.1.20:3000"
}
