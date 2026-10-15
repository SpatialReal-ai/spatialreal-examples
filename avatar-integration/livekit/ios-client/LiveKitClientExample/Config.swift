// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
enum Config {
    static let appId = "YOUR_APP_ID"
    /// The same avatar your agent uses
    static let avatarId = "YOUR_AVATAR_ID"

    /// The token server from ../web-client (`npm run server`). It hands the app a LiveKit room
    /// token and a SpatialReal session token; your API keys stay on the server.
    /// localhost works on the simulator; on a device, use your computer's address on the network.
    static let tokenServerURL = "http://localhost:3000"
}
