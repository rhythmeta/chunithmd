import SwiftUI

struct OtogameLoginView: View {
    let connected: Bool
    let onAuthorization: @MainActor (String) -> Void
    @State private var webViewIdentity = UUID()

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Label(connected ? tr("Otogame 会话已就绪") : tr("请先登录 Otogame"),
                      systemImage: connected ? "checkmark.circle.fill" : "person.crop.circle.badge.exclamationmark")
                    .foregroundStyle(connected ? .green : .secondary)
                Spacer()
                Button(tr("重新加载"), systemImage: "arrow.clockwise") { webViewIdentity = UUID() }
                    .labelStyle(.iconOnly)
                    .tint(.primary)
                    .accessibilityIdentifier("otogame-reload")
            }
            .padding()
            Divider()
            OtogameWebView(onAuthorizationHeader: onAuthorization).id(webViewIdentity)
        }
        .navigationTitle(tr("登录 Otogame"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
