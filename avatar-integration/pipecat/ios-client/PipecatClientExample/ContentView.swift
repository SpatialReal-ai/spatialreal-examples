import SpatialRealPipecat
import SwiftUI

struct ContentView: View {
    @StateObject private var model = PipecatModel()

    var body: some View {
        VStack(spacing: 16) {
            AvatarContainer(view: model.container)
                .aspectRatio(4 / 5, contentMode: .fit)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            HStack {
                Button("Start") { Task { await model.start() } }
                    .disabled(model.state != .idle || model.busy)
                Button("Leave") { Task { await model.leave() } }
                    .disabled(model.state != .live || model.busy)
            }

            Text(status)
                .foregroundStyle(.secondary)
            if let message = model.message {
                Text(message).foregroundStyle(.red)
            }
        }
        .buttonStyle(.bordered)
        .padding()
        // Create the session once the view is on screen
        .task { await model.create() }
        .onDisappear { Task { await model.release() } }
    }

    private var status: String {
        guard let state = model.state else { return "Session: loading" }
        guard state == .live else { return "Session: \(state.rawValue)" }
        guard model.botReady else { return "Session: live · waiting for the bot" }
        let voice = model.voiceRoute == .avatar ? "the avatar" : "the call"
        return "Session: live · say something · voice from \(voice)"
    }
}

/// Shows the UIView the SDK draws into
struct AvatarContainer: UIViewRepresentable {
    let view: UIView
    func makeUIView(context: Context) -> UIView { view }
    func updateUIView(_ uiView: UIView, context: Context) {}
}
