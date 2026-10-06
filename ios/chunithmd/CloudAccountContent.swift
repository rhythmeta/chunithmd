import SwiftUI

struct CloudAccountContent: View {
    let state: RhythmetaViewState
    let authenticate: (String) -> Void
    let backup: () -> Void
    let refresh: () -> Void
    let restore: (RhythmetaViewState.Backup) -> Void
    let logout: () -> Void

    var body: some View {
        List {
            Section {
                CloudAccountSummary(user: state.user)
            }
            .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
            .listRowBackground(Color.clear)
            .listSectionSeparator(.hidden)
            if let user = state.user {
                Section(tr("账户")) {
                    LabeledContent(tr("账号"), value: user.handle)
                    LabeledContent(tr("邮箱"), value: user.email)
                    LabeledContent(tr("状态")) {
                        Text(tr("已登录")).foregroundStyle(.secondary)
                    }
                }
                Section {
                    SettingsActionRow(title: tr("备份到云端"), icon: "icloud.and.arrow.up.fill", color: .blue, action: backup)
                        .accessibilityIdentifier("cloud-backup")
                    SettingsActionRow(title: tr("刷新备份列表"), icon: "arrow.clockwise", color: .green, action: refresh)
                        .accessibilityIdentifier("cloud-refresh")
                    if state.busy { ProgressView().accessibilityLabel(tr("正在处理备份…")) }
                } footer: {
                    Text(tr("手动创建备份，保留最近三份。持有下载链接的人可读取备份，请勿分享。"))
                }
                Section(tr("云端备份")) {
                    if state.backups.isEmpty {
                        Text(tr("暂无云备份。")).foregroundStyle(.secondary)
                    }
                    ForEach(state.backups) { item in
                        CloudBackupRestoreButton(backup: item) { restore(item) }
                    }
                }
                Section {
                    Button(tr("退出登录"), role: .destructive, action: logout)
                        .accessibilityIdentifier("cloud-logout")
                }
            } else {
                Section {
                    SettingsActionRow(title: tr("登录"), icon: "person.crop.circle.badge.checkmark", color: .blue) { authenticate("login") }
                        .accessibilityIdentifier("cloud-login")
                    SettingsActionRow(title: tr("注册"), icon: "person.badge.plus.fill", color: .green) { authenticate("register") }
                        .accessibilityIdentifier("cloud-register")
                    SettingsActionRow(title: tr("忘记密码"), icon: "key.fill", color: .orange) { authenticate("forgot") }
                        .accessibilityIdentifier("cloud-forgot")
                    if state.busy { ProgressView() }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
