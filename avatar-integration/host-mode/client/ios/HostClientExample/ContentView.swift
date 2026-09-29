import SwiftUI

struct ContentView: View {
    @StateObject private var model = HostModel()

    var body: some View {
        VStack(spacing: 16) {
            AvatarContainer(view: model.container)
                .aspectRatio(4 / 5, contentMode: .fit)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            HStack {
                Button("Start") { Task { await model.start() } }
                    .disabled(model.state != .idle)
                Button("Stop") { model.stop() }
                    .disabled(model.state != .live)
            }

            HStack {
                Button("Speak (female voice)") { model.speak("female") }
                Button("Speak (male voice)") { model.speak("male") }
            }
            .disabled(model.state != .live || !model.serverConnected)

            Text("Session: \(model.state?.rawValue ?? "loading") · Server: \(model.serverConnected ? "connected" : "not connected")")
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
