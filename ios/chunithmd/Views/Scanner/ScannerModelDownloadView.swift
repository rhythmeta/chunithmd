import SwiftUI

struct ScannerModelDownloadView: View {
    let models: ScannerModelController

    var body: some View {
        VStack(spacing: 14) {
            switch models.state.stage {
            case "loading": EmptyView()
            case "checking": ProgressView(tr("正在检查识别模型…"))
            case "downloading":
                ProgressView(value: Double(models.state.downloadedBytes), total: Double(max(1, models.state.totalBytes))) {
                    Text(tr("正在下载识别模型…"))
                } currentValueLabel: {
                    Text(size(models.state.downloadedBytes) + " / " + size(models.state.totalBytes))
                }
                Button(tr("取消"), action: models.cancel)
            case "failed":
                Text(tr("模型下载失败"))
                Text(tr("请检查网络连接后重试。")).foregroundStyle(.secondary)
                Button(tr("重试"), action: models.check)
            case "ready":
                Label(models.state.offline ? tr("现有模型仍可离线使用。") : tr("模型已就绪"), systemImage: "checkmark.circle")
                Button(tr("检查模型更新"), action: models.check)
            default:
                Text(models.state.usable ? tr("识别模型有更新") : tr("首次扫描需要下载模型，下载后可离线识别。"))
                Button((models.state.usable ? tr("更新模型") : tr("下载模型")) + " · " + size(models.state.totalBytes), action: models.download)
            }
            if models.state.usable && models.state.stage != "ready" {
                Text(tr("现有模型仍可离线使用。")).font(.caption).foregroundStyle(.secondary)
            }
        }
        .padding(20)
        .frame(maxWidth: 360)
        .background(.regularMaterial, in: .rect(cornerRadius: 24))
        .padding(24)
    }

    private func size(_ bytes: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: bytes, countStyle: .file)
    }
}
