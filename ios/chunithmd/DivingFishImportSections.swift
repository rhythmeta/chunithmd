import SwiftUI

struct DivingFishImportSections: View {
    let state: ScoreImportStore.Provider
    let authorize: () -> Void
    let sync: () -> Void
    let disconnect: () -> Void

    var body: some View {
        Section {
            Label {
                VStack(alignment: .leading) {
                    Text(state.connected ? tr("已连接水鱼账号") : tr("连接水鱼查分器")).bold()
                    Text(state.connected ? tr("已获得成绩读取授权，可直接同步成绩。") : tr("授权水鱼账号并导入中二节奏成绩"))
                        .foregroundStyle(.secondary)
                }
            } icon: {
                Image(systemName: state.connected ? "checkmark.shield.fill" : "person.badge.key.fill")
                    .foregroundStyle(state.connected ? .green : .blue)
            }
        }
        Section {
            if state.connected {
                ScoreImportActionRow(title: tr("同步成绩"), symbol: "arrow.triangle.2.circlepath", tint: .blue, action: sync)
                    .accessibilityIdentifier("import-sync")
                ScoreImportActionRow(title: tr("重新授权"), symbol: "person.badge.key", tint: .orange, action: authorize)
                ScoreImportActionRow(title: tr("断开连接"), symbol: "personalhotspot.slash", tint: .red, action: disconnect)
            } else {
                ScoreImportActionRow(title: tr("连接并导入"), symbol: "person.badge.key", tint: .blue, action: authorize)
                    .accessibilityIdentifier("import-authorize")
            }
        } header: {
            Text(tr("OAuth 授权"))
        } footer: {
            Text(tr("授权将在水鱼查分器网页中完成，chunithmd 不会读取或保存你的密码。"))
        }
        .disabled(state.busy)
        .opacity(state.busy ? 0.6 : 1)
    }
}
