import SwiftUI

struct ContentView: View {
    @StateObject private var model = LiveKitModel()

    var body: some View {
        VStack(spacing: 16) {
            AvatarContainer(view: model.container)
                .aspectRatio(4 / 5, contentMode: .fit)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            HStack {
                Button("Start") { Task { await model.start() } }
                    .disabled(model.state != .idle || model.busy)
                Button(model.muted ? "Unmute" : "Mute") { Task { await model.toggleMute() } }
                    .disabled(model.state != .live || model.busy)
                Button("Leave") { Task { await model.leave() } }
                    .disabled(model.state != .live || model.busy)
            }

            Text("Session: \(model.state?.rawValue ?? "loading")")
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
}

/// Shows the UIView the SDK draws into
struct AvatarContainer: UIViewRepresentable {
    let view: UIView
    func makeUIView(context: Context) -> UIView { view }
    func updateUIView(_ uiView: UIView, context: Context) {}
}
