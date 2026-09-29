import SpatialRealSDK
import SwiftUI

/// Owns the Host mode session and the connection to your server.
/// Follows "Host mode client" (iOS tab) in the docs.
@MainActor
final class HostModel: ObservableObject {
    /// The SDK draws into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var serverConnected = false
    @Published private(set) var message: String?

    private var host: HostAvatarSession?
    private var ws: URLSessionWebSocketTask?

    /// Create a Host mode session: the token is used only to download the avatar.
    /// The SDK opens no connection of its own; everything arrives through your server.
    func create() async {
        guard host == nil else { return }
        do {
            let sr = try SpatialReal(appId: Config.appId)
            let host = try await sr.createSession(HostAvatarSessionOptions(
                avatarId: Config.avatarId,
                credential: Config.sessionToken,
                container: container,              // must be on screen and have a size
                audioFormat: try .pcm16(16000)     // the rate of the audio your server forwards
            ))
            _ = host.onState { [weak self] change in self?.state = change.current }
            _ = host.onError { [weak self] error in self?.message = Self.describe(error) }
            state = host.state
            self.host = host
            connect()
        } catch {
            message = Self.describe(error)
        }
    }

    /// start() opens no connection in Host mode; it gets playback ready.
    func start() async {
        message = nil
        do {
            try await host?.start()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Ask the server for speech.
    func speak(_ voice: String) {
        send(["type": "speak", "voice": voice])
    }

    /// Stop playback here and tell the server, so it stops forwarding the old utterance.
    func stop() {
        try? host?.interrupt()
        send(["type": "interrupt"])
    }

    /// The screen goes away: close the connection and release everything.
    func release() async {
        ws?.cancel(with: .goingAway, reason: nil)
        await host?.dispose()
        host = nil
    }

    // MARK: - Your server

    private func connect() {
        guard let url = URL(string: Config.hostServerURL) else { return }
        let ws = URLSession.shared.webSocketTask(with: url)
        self.ws = ws
        ws.resume()
        ws.sendPing { [weak self] error in
            let connected = error == nil
            Task { @MainActor in self?.serverConnected = connected }
        }
        Task { await receive(from: ws) }
    }

    /// Hand over every message as it arrives: audio to yieldAudioData, motion to yieldFramesData.
    private func receive(from ws: URLSessionWebSocketTask) async {
        while let incoming = try? await ws.receive() {
            guard case .string(let text) = incoming,
                  let msg = try? JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any],
                  let host
            else { continue }
            do {
                switch msg["type"] as? String {
                case "audio":
                    let audio = Data(base64Encoded: msg["data"] as? String ?? "") ?? Data()
                    try host.yieldAudioData(audio, end: msg["end"] as? Bool ?? false)
                case "motion":
                    let frames = (msg["data"] as? [String] ?? []).compactMap { Data(base64Encoded: $0) }
                    try host.yieldFramesData(frames)
                default:
                    break
                }
            } catch {
                message = Self.describe(error)
            }
        }
        serverConnected = false
    }

    private func send(_ msg: [String: String]) {
        guard let data = try? JSONSerialization.data(withJSONObject: msg),
              let text = String(data: data, encoding: .utf8)
        else { return }
        ws?.send(.string(text)) { _ in }
    }

    private static func describe(_ error: Error) -> String {
        if let error = error as? SpatialRealError {
            return "[\(error.code.rawValue)] \(error.message)"
        }
        return error.localizedDescription
    }
}
