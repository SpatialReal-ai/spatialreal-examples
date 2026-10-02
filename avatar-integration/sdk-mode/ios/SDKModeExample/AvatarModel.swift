import SpatialRealSDK
import SwiftUI

/// Owns the avatar session. Each method is one step of "SDK mode on iOS" in the docs.
@MainActor
final class AvatarModel: ObservableObject {
    /// The SDK draws into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var message: String?

    private var avatar: AvatarSession?

    /// Create the session: it checks the token, loads the avatar and shows it.
    /// It doesn't connect yet, so nothing is billed.
    func create() async {
        guard avatar == nil else { return }
        do {
            let sr = try SpatialReal(appId: Config.appId)
            let avatar = try await sr.createSession(AvatarSessionOptions(
                avatarId: Config.avatarId,
                credential: Config.sessionToken,
                container: container,              // must be on screen and have a size
                audioFormat: try .pcm16(16000)     // the rate of the audio you will send
            ))
            _ = avatar.onState { [weak self] change in self?.state = change.current }
            _ = avatar.onError { [weak self] error in self?.message = Self.describe(error) }
            state = avatar.state
            self.avatar = avatar
        } catch {
            message = Self.describe(error)
        }
    }

    /// Returns once the session is live. From here on, the session is billed.
    func start() async {
        message = nil
        do {
            try await avatar?.start()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Send the whole clip in one call. `end: true` marks the end of the utterance.
    func speak(_ clip: String) {
        do {
            try avatar?.send(try AudioClip.pcm16(clip), end: true)
        } catch {
            message = Self.describe(error)
        }
    }

    /// The user talks over the avatar: stop at once.
    func stop() {
        try? avatar?.interrupt()
    }

    /// Close the session but keep the avatar on screen; start() opens it again.
    func end() async {
        try? await avatar?.end()
    }

    /// The screen goes away: release everything.
    func release() async {
        await avatar?.dispose()
        avatar = nil
    }

    private static func describe(_ error: Error) -> String {
        if let error = error as? SpatialRealError {
            return "[\(error.code.rawValue)] \(error.message)"
        }
        return error.localizedDescription
    }
}
