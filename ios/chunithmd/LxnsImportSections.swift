import SwiftUI

struct LxnsImportSections: View {
    let state: ScoreImportStore.Provider
    @Binding var code: String
    let authorize: () -> Void
    let sync: () -> Void
    let disconnect: () -> Void
    let exchange: () -> Void

    var body: some View {
        Section {
            LxnsImportSummary(connected: state.connected)
        }
        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
        .listSectionSeparator(.hidden)

        if state.connected {
            Section(tr("已绑定 LXNS")) {
                HStack {
                    ScoreImportIcon(symbol: "checkmark.shield.fill", tint: .green)
                    Text(tr("授权状态"))
                    Spacer()
                    Text(tr("已连接")).foregroundStyle(.green)
                }
                ScoreImportActionRow(title: state.busy ? tr("正在同步…") : tr("同步成绩"),
                                     symbol: "arrow.triangle.2.circlepath.circle.fill", tint: .cyan, action: sync)
                    .disabled(state.busy)
                    .opacity(state.busy ? 0.6 : 1)
                    .accessibilityIdentifier("import-sync")
            }
            Section(tr("账号管理")) {
                Button(tr("断开连接并重新登录"), role: .destructive, action: disconnect).disabled(state.busy)
            }
        } else {
            Section {
                ScoreImportActionRow(title: tr("在浏览器中打开授权页面"), symbol: "safari.fill", tint: .indigo, action: authorize)
                    .disabled(state.busy)
                    .opacity(state.busy ? 0.6 : 1)
                    .accessibilityIdentifier("import-authorize")
            } header: {
                Text(tr("第一步：获取授权码"))
            } footer: {
                Text(tr("前往落雪咖啡屋登录并授权，完成后复制页面上的授权码。"))
            }
            Section {
                VStack {
                    HStack {
                        ScoreImportIcon(symbol: "key.fill", tint: .gray)
                        TextField(tr("粘贴浏览器中的授权码"), text: $code)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .disabled(state.busy)
                            .accessibilityIdentifier("import-code")
                    }
                    Button(action: exchange) {
                        HStack {
                            Spacer()
                            if state.busy { ProgressView() }
                            Text(state.busy ? tr("正在同步…") : tr("开始导入")).bold()
                            Spacer()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(state.busy || code.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("import-exchange")
                }
                .padding(.vertical, 4)
                .listRowSeparator(.hidden)
            } header: {
                Text(tr("第二步：输入授权码并导入"))
            } footer: {
                Text(tr("将授权码粘贴到上方，点击开始导入即可同步成绩。"))
            }
            .listSectionSeparator(.hidden)
        }
    }
}

private struct LxnsImportSummary: View {
    let connected: Bool
    @ScaledMetric(relativeTo: .largeTitle) private var iconSize = 72

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Image(systemName: connected ? "snowflake.circle.fill" : "link.badge.plus")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                    .frame(width: iconSize, height: iconSize)
                    .background((connected ? Color.cyan : Color.indigo).gradient, in: .rect(cornerRadius: 18))
                    .accessibilityHidden(true)
                VStack(alignment: .leading) {
                    Text(connected ? tr("已绑定 LXNS") : tr("第一步：获取授权码"))
                        .font(.headline).fontDesign(.rounded)
                    Text(connected ? tr("已连接") : tr("前往落雪咖啡屋登录并授权，完成后复制页面上的授权码。"))
                        .font(.subheadline).foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            Divider()
            Label(tr("导入至当前档案，已有成绩会保留。"), systemImage: "square.and.arrow.down")
                .font(.footnote).foregroundStyle(.secondary)
        }
        .padding(20)
    }
}
