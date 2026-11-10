enum Config {
    /// The token server from ../web-client (`npm run server`). It hands the app everything it
    /// needs: the Agora App ID, channel, uids and RTC token, and your SpatialReal App ID, avatar ID
    /// and a session token. Your API key and App Certificate stay on the server, in its `.env`.
    /// localhost works on the simulator; on a device, use your computer's address on the network.
    static let tokenServerURL = "http://localhost:3000"
}
