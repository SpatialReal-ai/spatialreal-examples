// Your values, from SpatialReal Studio (https://app.spatialreal.ai)
enum Config {
    static let appId = "YOUR_APP_ID"
    /// The same avatar your Host mode server uses
    static let avatarId = "YOUR_AVATAR_ID"

    /// A session token, used only to download the avatar. For a first try, create a temporary
    /// token in Studio and paste it here. A real app fetches one from its own server instead:
    /// https://docs.spatialreal.ai/overview/session-tokens
    static let sessionToken = "YOUR_SESSION_TOKEN"

    /// Your Host mode server. localhost works on the simulator; on a device, use your computer's
    /// address on the network, and start the server with HOST=0.0.0.0.
    static let hostServerURL = "ws://localhost:8765"
}
