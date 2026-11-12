import AVFoundation
import PipecatClientIOS
import PipecatClientIOSDaily
import SpatialRealPipecat
import SpatialRealSDK
import SwiftUI

/// Owns the Pipecat avatar session and Pipecat's client. The app joins your bot's Daily room,
/// sends the user's microphone to the bot, and renders the avatar that speaks the bot's replies.
/// The app sends no speech to SpatialReal and opens no connection of its own to it: the bot's
/// voice and the animation both arrive through the call.
@MainActor
final class PipecatModel: ObservableObject {
    /// The SDK draws into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var voiceRoute: PipecatVoiceRoute = .call
    @Published private(set) var botReady = false
    @Published private(set) var message: String?
    @Published private(set) var busy = false

    /// The avatar session and Pipecat's client share this transport, for the life of the screen.
    private let transport = DailyTransport()
    private var avatar: PipecatAvatarSession?
    private var client: PipecatClient?
    private var inCall = false
    private var removeListeners: [@MainActor () -> Void] = []

    /// Fetch a session token from your server, then create the session: the avatar is downloaded
    /// and shown, standing still. Nothing connects yet.
    func create() async {
        guard avatar == nil else { return }
        do {
            let sessionToken = try await fetchSessionToken()
            let sr = try SpatialReal(appId: Config.appId)
            let avatar = try await sr.createSession(PipecatAvatarSessionOptions(
                avatarId: Config.avatarId,   // the avatar your bot's processor uses
                credential: sessionToken,    // downloads the avatar
                container: container,        // must be on screen and have a size
                transport: transport
            ))
            let removeState = avatar.onState { [weak self] change in self?.state = change.current }
            let removeError = avatar.onError { [weak self] error in self?.message = Self.describe(error) }
            // .call: Daily plays the bot's track. .avatar: the avatar plays the bot's voice
            let removeRoute = avatar.onVoiceRouteChange { [weak self] route in self?.voiceRoute = route }
            removeListeners = [{ removeState() }, { removeError() }, { removeRoute() }]
            state = avatar.state
            voiceRoute = avatar.voiceRoute
            self.avatar = avatar
        } catch {
            message = Self.describe(error)
        }
    }

    /// Start the avatar, then ask Pipecat's development runner for a bot in a new Daily room and
    /// join it. The bot greets you; from its first pause on, the avatar speaks its replies.
    func start() async {
        guard let avatar, !busy else { return }
        busy = true
        defer { busy = false }
        message = nil
        guard await AVCaptureDevice.requestAccess(for: .audio) else {
            message = "Microphone access is required. Enable it in Settings > Privacy & Security > Microphone."
            return
        }
        do {
            try await avatar.start()   // no network: it only starts the avatar's player

            guard let runner = URL(string: Config.botServerURL)?.appending(path: "start") else {
                throw ConfigError.badURL("Config.botServerURL")
            }
            // A new client for every conversation, on the same transport
            let client = PipecatClient(options: PipecatClientOptions(transport: transport, enableMic: true))
            client.delegate = self
            self.client = client
            inCall = true
            let _: DailyTransportConnectionParams = try await client.startBotAndConnect(
                startBotParams: APIRequest(endpoint: runner, requestData: .object(["createDailyRoom": .boolean(true)]))
            )
        } catch {
            message = "Could not reach the bot at \(Config.botServerURL): is `uv run bot.py -t daily` running? (\(Self.describe(error)))"
            await leave()
        }
    }

    /// Leave the call and keep the avatar on screen; Start begins a new conversation.
    func leave() async {
        inCall = false
        botReady = false
        // End the avatar first, while the call can still switch the bot's audio track back on
        if avatar?.state == .live { try? await avatar?.end() }
        try? await client?.disconnect()   // the transport stays, for the next Start
    }

    /// The screen goes away: leave the call and release everything.
    func release() async {
        await leave()
        client?.release()
        client = nil
        removeListeners.forEach { $0() }
        removeListeners = []
        let previous = avatar
        avatar = nil
        await previous?.dispose()
    }

    // MARK: - Your token server

    /// What `POST /api/session-token` returns: see ../web-client/server.js
    private struct SessionToken: Decodable {
        let sessionToken: String
    }

    private func fetchSessionToken() async throws -> String {
        guard let base = URL(string: Config.tokenServerURL) else { throw ConfigError.badURL("Config.tokenServerURL") }
        var request = URLRequest(url: base.appending(path: "api/session-token"))
        request.httpMethod = "POST"
        let configuration = URLSessionConfiguration.default
        // Keep the first request alive while the user answers the local-network prompt.
        configuration.waitsForConnectivity = true
        configuration.timeoutIntervalForResource = 60
        let session = URLSession(configuration: configuration)
        defer { session.finishTasksAndInvalidate() }
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw ConfigError.tokenServer((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
        return try JSONDecoder().decode(SessionToken.self, from: data).sessionToken
    }

    private enum ConfigError: LocalizedError {
        case badURL(String)
        case tokenServer(Int)

        var errorDescription: String? {
            switch self {
            case .badURL(let name): return "\(name) is not a URL"
            case .tokenServer(let status): return "Token server replied \(status). Is `npm run server` running in ../web-client, and is the URL reachable from this device?"
            }
        }
    }

    private static func describe(_ error: Error) -> String {
        if let error = error as? SpatialRealError {
            return "[\(error.code.rawValue)] \(error.message)"
        }
        return error.localizedDescription
    }
}

// MARK: - Pipecat's callbacks

// PipecatClient calls its delegate on the main actor
extension PipecatModel: @preconcurrency PipecatClientDelegate {
    /// Lets the session ask your bot's processor to send the bot's voice with the animation
    func onBotReady(botReadyData: BotReadyData) {
        avatar?.handleBotReady()
        botReady = true
    }

    /// Every RTVI server message goes to the session: it takes the avatar's and returns false
    /// for anything else, so your own handling can sit behind it.
    func onServerMessage(data: Any) {
        if avatar?.handleServerMessage(data) == true { return }
    }

    /// The bot left, or the call ended: keep the avatar on screen, ready for Start
    func onDisconnected() {
        guard inCall else { return }
        Task { await leave() }
    }
}
