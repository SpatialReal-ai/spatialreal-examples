import SpatialRealSDK
import SwiftUI

struct ContentView: View {
    @StateObject private var model = ChatModel()

    var body: some View {
        VStack(spacing: 12) {
            AvatarContainer(view: model.container)
                .aspectRatio(4 / 5, contentMode: .fit)
                .frame(maxHeight: 420)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            HStack {
                Button("Start") { Task { await model.start() } }
                    .disabled(model.state != .idle)
                Button("End") { Task { await model.end() } }
                    .disabled(model.state != .live)
                Spacer()
                Text(model.state?.rawValue ?? "loading")
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.bordered)

            if let message = model.message {
                Text(message).foregroundStyle(.red)
            }

            ScrollView {
                VStack(spacing: 6) {
                    ForEach(model.lines) { line in
                        Text(line.text)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(line.role == .user ? Color.blue.opacity(0.15) : Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .frame(maxWidth: .infinity, alignment: line.role == .user ? .trailing : .leading)
                    }
                }
            }
        }
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
