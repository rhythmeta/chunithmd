import SwiftUI

struct ProfileRow: View {
    let profile: PersonalSnapshot.Profile
    let version: String?

    var body: some View {
        HStack(spacing: 14) {
            ProfileAvatarView(data: Data(profile.avatar.map { UInt8(bitPattern: $0) }))
                .frame(width: 48, height: 48)
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(profile.name.isEmpty ? tr("我的档案") : profile.name)
                        .font(.headline)
                    if profile.active {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(.green).font(.caption).accessibilityHidden(true)
                    }
                }
                HStack(spacing: 6) {
                    Text(AppTheme.serverName(profile.server))
                        .font(.caption)
                        .padding(.horizontal, 6).padding(.vertical, 2)
                        .foregroundStyle(AppTheme.serverColor(profile.server))
                        .background(AppTheme.serverColor(profile.server).opacity(0.15), in: .capsule)
                    if let version {
                        Text(version).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
            Spacer()
            if profile.active {
                Text(tr("当前档案")).font(.caption2).foregroundStyle(.green)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(.green.opacity(0.1), in: .capsule)
            }
        }
        .foregroundStyle(.primary)
        .padding(.vertical, 4)
        .contentShape(.rect)
    }
}
