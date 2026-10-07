import SwiftUI

struct CloudAccountSummary: View {
    let user: RhythmetaViewState.User?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                Image(systemName: user == nil ? "person.badge.key.fill" : "person.crop.circle.badge.checkmark")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 72, height: 72)
                    .background(.blue.gradient, in: .rect(cornerRadius: 18))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 6) {
                    Text(user?.handle ?? tr("Rhythmeta")).font(.headline.bold())
                    Text(user?.email ?? tr("登录 Rhythmeta 账号以备份和恢复数据。"))
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
            Divider()
            Label(tr("Rhythmeta 账号可用于 maimaid 和 chunithmd，备份需要手动创建。"), systemImage: "lock.shield.fill")
                .font(.footnote).foregroundStyle(.secondary)
        }
        .padding(20)
        .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 28))
        .accessibilityIdentifier("cloud-account-summary")
    }
}
