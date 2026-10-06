import SwiftUI

struct HomeFeatureCard: View {
    let destination: HomeDestination
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: destination.icon)
                .font(.system(size: 28))
                .foregroundStyle(LinearGradient(colors: destination.gradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 32, height: 32, alignment: .leading)
            VStack(alignment: .leading, spacing: 4) {
                Text(destination.title).font(.system(size: 15, weight: .semibold)).foregroundStyle(.primary)
                Text(destination.subtitle).font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppTheme.surface, in: .rect(cornerRadius: 16))
        .overlay { RoundedRectangle(cornerRadius: 16).stroke(AppTheme.border) }
        .shadow(color: .black.opacity(0.02), radius: 8, y: 4)
    }
}
