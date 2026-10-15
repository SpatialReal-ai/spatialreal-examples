import AVFoundation
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

    @Published private(set) var busy = false

    private var needsNewRoom = false
    private var removeStateListener: (@MainActor () -> Void)?
    private var removeErrorListener: (@MainActor () -> Void)?
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
            let removeState = session.onState { [weak self] change in self?.state = change.current }
            removeStateListener = { removeState() }
            let removeError = session.onError { [weak self] error in self?.message = Self.describe(error) }
            removeErrorListener = { removeError() }
            state = session.state
            self.session = session
        } catch {
            message = Self.describe(error)
        }
    }

    /// start() joins the room and turns on the microphone. Your agent joins the new room
    /// and greets you; the avatar speaks its replies.
    func start() async {
        guard !busy else { return }
        busy = true
        defer { busy = false }
        message = nil
        do {
            let allowed = await AVCaptureDevice.requestAccess(for: .audio)
            guard allowed else {
                message = "Microphone access is required. Enable it in Settings > Privacy & Security > Microphone."
                return
            }
            if needsNewRoom {
                // The previous agent ends on Leave. A new room dispatches a new agent.
                state = nil
                await release()
                await create()
                guard session != nil else {
                    state = .idle
                    return
                }
                needsNewRoom = false
            }
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
        guard !busy else { return }
        busy = true
        needsNewRoom = true
        defer { busy = false }
        do {
            try await session?.end()
        } catch {
            message = Self.describe(error)
        }
        muted = false
    }

    /// The screen goes away: leave the room and release everything.
    func release() async {
        removeStateListener?()
        removeErrorListener?()
        removeStateListener = nil
        removeErrorListener = nil
        let previous = session
        session = nil
        await previous?.dispose()
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
