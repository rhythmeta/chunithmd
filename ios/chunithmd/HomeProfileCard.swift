import SwiftUI

struct HomeProfileCard: View {
    @Environment(PersonalStore.self) private var personal
    let edit: () -> Void
    var body: some View {
        let profile = personal.snapshot.activeProfile
        Button(action: edit) {
            HStack(spacing: 16) {
                ProfileAvatarView(data: profile.map { Data($0.avatar.map { UInt8(bitPattern: $0) }) })
                    .frame(width: 60, height: 60)
                    .overlay(alignment: .bottomTrailing) {
                        Text(personal.rating, format: .number.precision(.fractionLength(2)))
                            .font(.system(size: 10, weight: .black, design: .rounded))
                            .foregroundStyle(.white).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(.orange, in: .capsule)
                            .overlay { Capsule().stroke(.white, lineWidth: 1) }
                            .offset(x: 4, y: 4)
                    }
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(profile?.name ?? tr("我的档案")).font(.system(size: 18, weight: .bold))
                        Text(AppTheme.serverName(profile?.server ?? "jp"))
                            .font(.system(size: 10, weight: .bold)).foregroundStyle(.white)
                            .padding(.horizontal, 5).padding(.vertical, 2)
                            .background(AppTheme.serverColor(profile?.server ?? "jp"), in: .rect(cornerRadius: 4))
                    }
                    Text(profile?.title.isEmpty == false ? profile?.title ?? "" : tr("点击编辑个人资料"))
                        .font(.system(size: 12)).foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").font(.system(size: 14, weight: .bold)).foregroundStyle(.secondary.opacity(0.3))
            }
            .padding(16)
            .background(AppTheme.surface, in: .rect(cornerRadius: 20))
            .overlay { RoundedRectangle(cornerRadius: 20).stroke(AppTheme.border) }
            .shadow(color: .black.opacity(0.02), radius: 8, y: 4)
        }.buttonStyle(.plain)
    }
}
