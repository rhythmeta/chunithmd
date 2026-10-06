import SwiftUI

struct CommunityMessageCard<Action: View>: View {
    let title: String
    let message: String
    @ViewBuilder var action: () -> Action

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            action()
        }
        .padding(.vertical, 4)
    }
}
