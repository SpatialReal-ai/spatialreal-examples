// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
enum Config {
    static let appId = "YOUR_APP_ID"
    /// The same avatar your bot's processor uses
    static let avatarId = "YOUR_AVATAR_ID"

    /// Pipecat's development runner from ../bot (`uv run bot.py -t daily --host 0.0.0.0`).
    /// `POST /start` creates a Daily room and starts a bot in it.
    static let botServerURL = "http://localhost:7860"

    /// The token server from ../web-client (`npm run server`). It hands the app a SpatialReal
    /// session token to download the avatar; your API key stays on the server.
    static let tokenServerURL = "http://localhost:3000"

    // localhost works on the simulator; on a device, use your computer's address on the network,
    // for example http://192.168.1.20:7860 and http://192.168.1.20:3000.
}
