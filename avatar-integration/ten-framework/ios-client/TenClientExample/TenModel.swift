import AgoraRtcKit
import SpatialRealAgora
import SpatialRealSDK
import SwiftUI

/// Owns the Agora avatar session. The app joins your Agora channel, sends the user's microphone
/// to your agent, and renders the avatar that SpatialReal publishes into the channel. The app
/// sends no speech of its own to SpatialReal — your agent does.
@MainActor
final class TenModel: ObservableObject {
    /// The SDK draws into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var muted = false
    @Published private(set) var message: String?
    @Published private(set) var channel: String?

    @Published private(set) var busy = false

    private var removeStateListener: (@MainActor () -> Void)?
    private var removeErrorListener: (@MainActor () -> Void)?
    private var renewal: Task<Void, Never>?
    private var session: AgoraAvatarSession?

    /// Ask your server what to join, then create the session: the avatar is downloaded and
    /// shown, standing still. Nothing joins the channel yet.
    func create() async {
        guard session == nil else { return }
        do {
            let cfg: SessionConfig = try await post("api/session")
            channel = cfg.channel
            let sr = try SpatialReal(appId: cfg.spatialrealAppId)
            let session = try await sr.createSession(AgoraAvatarSessionOptions(
                avatarId: cfg.avatarId,
                credential: cfg.credential,                                   // downloads the avatar
                container: container,                                         // must be on screen and have a size
                // avatarUid is the uid your agent publishes the avatar with; uid is this app's
                agora: .join(appId: cfg.appId, channel: cfg.channel, token: cfg.token, avatarUid: cfg.avatarUid, uid: cfg.uid),
                mic: .required                                                // start() publishes the microphone
            ))
            let removeState = session.onState { [weak self] change in
                self?.state = change.current
                if change.current == .live { self?.renewTokenBeforeExpiry() } else { self?.renewal?.cancel() }
            }
            removeStateListener = { removeState() }
            let removeError = session.onError { [weak self] error in self?.message = Self.describe(error) }
            removeErrorListener = { removeError() }
            state = session.state
            self.session = session
        } catch {
            message = Self.describe(error)
        }
    }

    /// start() joins the channel and publishes the microphone; the first time, iOS asks for it.
    /// Your agent hears you, and the avatar speaks its replies.
    func start() async {
        guard !busy else { return }
        busy = true
        defer { busy = false }
        message = nil
        do {
            try await session?.start()
        } catch let error as SpatialRealError where error.code == .micDenied {
            message = "Microphone access is required. Enable it in Settings > Privacy & Security > Microphone."
        } catch {
            message = Self.describe(error)
        }
    }

    /// Stop sending the microphone without leaving the channel.
    func toggleMute() async {
        guard let session else { return }
        do {
            try await session.setMicrophone(enabled: muted)
            muted.toggle()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Leave the channel. The avatar stays on screen; start() joins again.
    func end() async {
        guard !busy else { return }
        busy = true
        defer { busy = false }
        do {
            try await session?.end()
        } catch {
            message = Self.describe(error)
        }
        muted = false
    }

    /// The screen goes away: leave the channel and release everything.
    func release() async {
        renewal?.cancel()
        removeStateListener?()
        removeErrorListener?()
        removeStateListener = nil
        removeErrorListener = nil
        let previous = session
        session = nil
        await previous?.dispose()
    }

    /// The SDK never renews the Agora token, and it is the delegate of the engine it created, so
    /// the app doesn't see Agora's "token will expire" callback. The server's tokens last an hour
    /// (TOKEN_TTL_S in server.js): fetch a fresh one well before that, for as long as the app is
    /// in the channel.
    private func renewTokenBeforeExpiry() {
        renewal?.cancel()
        renewal = Task { [weak self] in
            while true {
                try? await Task.sleep(for: .seconds(45 * 60))
                guard !Task.isCancelled, let self, let session = self.session, session.state == .live else { return }
                do {
                    let fresh: RtcToken = try await self.post("api/agora-token")
                    session.engine.renewToken(fresh.token)
                } catch {
                    self.message = "Token renewal failed: \(Self.describe(error))"
                }
            }
        }
    }

    // MARK: - Your token server

    /// What `POST /api/session` returns: see ../web-client/server.js
    private struct SessionConfig: Decodable {
        // Agora: the project, the channel, this app's RTC token and uid, and the avatar's uid
        let appId: String
        let channel: String
        let token: String
        let avatarUid: UInt
        let uid: UInt
        // SpatialReal: the App ID, the avatar, and the session token that downloads it
        let spatialrealAppId: String
        let avatarId: String
        let credential: String
    }

    /// What `POST /api/agora-token` returns
    private struct RtcToken: Decodable {
        let token: String
    }

    private func post<Reply: Decodable>(_ path: String) async throws -> Reply {
        guard let base = URL(string: Config.tokenServerURL) else { throw TokenServerError.badURL }
        var request = URLRequest(url: base.appending(path: path))
        request.httpMethod = "POST"
        let configuration = URLSessionConfiguration.default
        // Keep the first request alive while the user answers the local-network prompt.
        configuration.waitsForConnectivity = true
        configuration.timeoutIntervalForResource = 60
        let client = URLSession(configuration: configuration)
        defer { client.finishTasksAndInvalidate() }
        let (data, response) = try await client.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw TokenServerError.failed((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
        return try JSONDecoder().decode(Reply.self, from: data)
    }

    private enum TokenServerError: LocalizedError {
        case badURL
        case failed(Int)

        var errorDescription: String? {
            switch self {
            case .badURL: return "Config.tokenServerURL is not a URL"
            case .failed(let status): return "Token server replied \(status). Is `npm run server` running in ../web-client with its .env filled in, and is the URL reachable from this device?"
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
