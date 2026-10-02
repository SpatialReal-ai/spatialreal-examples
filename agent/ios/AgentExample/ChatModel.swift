import SpatialRealSDK
import SwiftUI

/// Owns the conversation with the agent. Follows "Your own app" (iOS tab) in the docs.
@MainActor
final class ChatModel: ObservableObject {
    struct Line: Identifiable {
        let id: String
        let role: TranscriptRole
        var text: String
    }

    /// The SDK draws the agent's avatar into this view. SwiftUI shows it through AvatarContainer.
    let container = UIView()

    @Published private(set) var state: SessionState?
    @Published private(set) var lines: [Line] = []
    @Published private(set) var message: String?

    private var chat: ChatSession?

    /// Create the session: it checks the token and shows the agent's avatar.
    /// It doesn't connect yet, so nothing is billed.
    func create() async {
        guard chat == nil else { return }
        do {
            let sr = try SpatialReal(appId: Config.appId)
            let chat = try await sr.createSession(ChatSessionOptions(
                agentId: Config.agentId,
                credential: Config.sessionToken,
                container: container       // must be on screen and have a size; nil for voice only
            ))
            _ = chat.onTranscript { [weak self] event in self?.show(event) }
            _ = chat.onState { [weak self] change in self?.state = change.current }
            _ = chat.onError { [weak self] error in self?.report(error) }
            state = chat.state
            self.chat = chat
        } catch {
            message = Self.describe(error)
        }
    }

    /// Connects to the agent and opens the microphone. The first time, iOS asks the user for access.
    func start() async {
        message = nil
        lines = []
        do {
            try await chat?.start()
        } catch {
            message = Self.describe(error)
        }
    }

    /// Finish the conversation but keep the session: start() opens a new one.
    func end() async {
        try? await chat?.end()
    }

    /// The screen goes away: release everything.
    func release() async {
        await chat?.dispose()
        chat = nil
    }

    /// text is the whole utterance so far: replace this turn's line, don't append to it.
    private func show(_ event: TranscriptEvent) {
        let id = "\(event.turnId ?? ""):\(event.role.rawValue)"
        if let i = lines.firstIndex(where: { $0.id == id }) {
            lines[i].text = event.text
        } else {
            lines.append(Line(id: id, role: event.role, text: event.text))
        }
    }

    /// session-ended: the agent, a time limit or your credits ended the conversation.
    /// Don't reconnect on your own; let the user start again.
    private func report(_ error: SpatialRealError) {
        if error.code == .sessionEnded {
            message = "The conversation ended (\(error.reason ?? "no reason given"))."
        } else {
            message = Self.describe(error)
        }
    }

    private static func describe(_ error: Error) -> String {
        if let error = error as? SpatialRealError {
            return "[\(error.code.rawValue)] \(error.message)"
        }
        return error.localizedDescription
    }
}
