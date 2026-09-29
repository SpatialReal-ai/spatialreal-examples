import SwiftUI

struct ContentView: View {
    @StateObject private var model = AvatarModel()

    var body: some View {
        VStack(spacing: 16) {
            AvatarContainer(view: model.container)
                .aspectRatio(4 / 5, contentMode: .fit)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            HStack {
                Button("Start") { Task { await model.start() } }
                    .disabled(model.state != .idle)
                Button("End") { Task { await model.end() } }
                    .disabled(model.state != .live)
            }

            HStack {
                Button("Speak (female voice)") { model.speak("female") }
                Button("Speak (male voice)") { model.speak("male") }
                Button("Stop") { model.stop() }
            }
            .disabled(model.state != .live)

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
