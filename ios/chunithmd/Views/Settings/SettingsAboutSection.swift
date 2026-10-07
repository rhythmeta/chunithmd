import SwiftUI
import Shared

struct SettingsAboutSection: View {
    let account: RhythmetaAccountStore
    @State private var backendAvailable: Bool?
    @ScaledMetric(relativeTo: .subheadline) private var valueSize = 15

    private var backendText: String {
        switch backendAvailable {
        case nil: tr("检查中…")
        case true: tr("可用")
        case false: tr("不可用")
        }
    }

    private var backendColor: Color {
        switch backendAvailable {
        case nil: .gray
        case true: .green
        case false: .red
        }
    }

    private var versionText: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "—"
        return "\(version) (\(build))"
    }

    var body: some View {
        Section(tr("关于")) {
            LabeledContent {
                Text(backendText).font(.system(size: valueSize)).foregroundStyle(.secondary)
            } label: {
                SettingsRowLabel(title: tr("后端状态"), icon: "server.rack", color: backendColor)
            }
            .accessibilityIdentifier("settings-backend")
            LabeledContent {
                Text(versionText).font(.system(size: valueSize)).foregroundStyle(.secondary)
            } label: {
                SettingsRowLabel(title: tr("版本"), icon: "info.circle.fill", color: .gray)
            }
            .accessibilityIdentifier("settings-version")
        }
        .task {
            backendAvailable = nil
            let result = try? await account.bridge.isHealthy()
            guard !Task.isCancelled else { return }
            backendAvailable = result?.boolValue ?? false
        }
    }
}
