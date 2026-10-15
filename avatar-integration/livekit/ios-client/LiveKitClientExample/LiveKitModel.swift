import SpatialRealLiveKit
import SpatialRealSDK
import SwiftUI

/// Owns the LiveKit avatar session. The app joins your LiveKit room, sends the user's microphone
/// to your agent, and renders the avatar that SpatialReal publishes into the room. The app sends
/// no speech of its own to SpatialReal — your agent does.
@MainActor
final class LiveKitModel: ObservableObject {
    /// The SDK draws into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var muted = false
    @Published private(set) var message: String?

    private var session: LiveKitAvatarSession?

    /// Fetch the two tokens from your server, then create the session: the avatar is downloaded
    /// and shown, standing still. Nothing joins the room yet.
    func create() async {
        guard session == nil else { return }
        do {
            let tokens = try await fetchTokens()
            let sr = try SpatialReal(appId: Config.appId)
            let session = try await sr.createSession(LiveKitAvatarSessionOptions(
                avatarId: Config.avatarId,
                credential: tokens.sessionToken,                              // downloads the avatar
                container: container,                                         // must be on screen and have a size
                livekit: LiveKitConnection(url: tokens.url, token: tokens.roomToken),
                mic: .required                                                // start() turns the microphone on
            ))
            _ = session.onState { [weak self] change in self?.state = change.current }
            _ = session.onError { [weak self] error in self?.message = Self.describe(error) }
            state = session.state
            self.session = session
        } catch {
            message = Self.describe(error)
        }
    }

    /// start() joins the room and turns on the microphone. Your agent joins the new room
    /// and greets you; the avatar speaks its replies.
    func start() async {
        message = nil
        do {
            try await session?.start()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Stop sending the microphone without leaving the room.
    func toggleMute() async {
        guard let session else { return }
        do {
            try await session.setMicrophone(enabled: muted)
            muted.toggle()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Leave the room. The avatar stays on screen; start() joins again.
    func leave() async {
        do {
            try await session?.end()
        } catch {
            message = Self.describe(error)
        }
        muted = false
    }

    /// The screen goes away: leave the room and release everything.
    func release() async {
        await session?.dispose()
        session = nil
    }

    // MARK: - Your token server

    /// What `POST /api/tokens` returns: see ../web-client/server.js
    private struct Tokens: Decodable {
        let url: String
        let roomToken: String
        let sessionToken: String
    }

    private func fetchTokens() async throws -> Tokens {
        guard let base = URL(string: Config.tokenServerURL) else { throw TokenServerError.badURL }
        var request = URLRequest(url: base.appending(path: "api/tokens"))
        request.httpMethod = "POST"
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw TokenServerError.failed((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
        return try JSONDecoder().decode(Tokens.self, from: data)
    }

    private enum TokenServerError: LocalizedError {
        case badURL
        case failed(Int)

        var errorDescription: String? {
            switch self {
            case .badURL: return "Config.tokenServerURL is not a URL"
            case .failed(let status): return "Token server replied \(status). Is `npm run server` running in ../web-client, and is the URL reachable from this device?"
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
