import SwiftUI

struct OtogameLoginView: View {
    let onAuthorization: @MainActor (String) -> Void
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            OtogameWebView(onAuthorizationHeader: onAuthorization)
                .navigationTitle(tr("登录 Otogame")).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() } } }
        }
    }
}
