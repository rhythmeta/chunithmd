import SwiftUI

struct ScoreImportStatusSection: View {
    let state: ScoreImportStore.Provider
    let error: String?
    let cancel: () -> Void

    private var progressMessage: String {
        switch state.phase {
        case "Authorizing": tr("正在获取授权码…")
        case "Waiting": tr("在浏览器完成授权后，返回此页即可自动导入。")
        default: tr("正在同步…")
        }
    }

    var body: some View {
        if state.busy || state.error != nil || error != nil || state.result != nil {
            Section(tr("状态")) {
                VStack(alignment: .leading, spacing: 12) {
                    if state.busy {
                        ProgressView(progressMessage).foregroundStyle(.secondary)
                        if state.totalPages > 0 {
                            ProgressView(value: Double(state.page), total: Double(state.totalPages))
                            Text(tr("正在读取第 {0} / {1} 页", String(state.page), String(state.totalPages)))
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    if let code = state.code {
                        Text(code).font(.title.monospaced()).textSelection(.enabled)
                        Text(tr("请核对授权页上的用户码")).font(.caption).foregroundStyle(.secondary)
                    }
                    if state.phase == "Waiting", let value = state.url, let url = URL(string: value) {
                        Link(tr("打开授权页面"), destination: url)
                    }
                    if let error = state.error ?? error {
                        Label(error, systemImage: "xmark.circle.fill").foregroundStyle(.red).textSelection(.enabled)
                    }
                    if let result = state.result {
                        Label(result, systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green).textSelection(.enabled)
                    }
                }
                .padding(.vertical, 4)
                if state.busy {
                    Button(tr("取消"), systemImage: "xmark", role: .cancel, action: cancel)
                }
            }
        }
    }
}
