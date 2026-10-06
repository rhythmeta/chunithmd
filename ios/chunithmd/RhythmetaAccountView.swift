import AuthenticationServices
import Foundation
import Observation
import Shared
import SwiftUI

struct RhythmetaViewState: Decodable {
    struct User: Decodable { let handle: String; let email: String }
    struct Backup: Decodable, Identifiable { let id: String; let deviceName: String; let committedAt: String; let profileCount: Int; let size: Int }
    var user: User?
    var backups: [Backup] = []
    var busy = false
    var ready = false
    var error: String?
}

@MainActor @Observable
final class RhythmetaAccountStore {
    let bridge = RhythmetaBridge(secrets: RhythmetaKeychain(), files: RhythmetaSnapshotFiles(), clientVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1")
    var state = RhythmetaViewState()
    var loginError: String?
    var community = CommunityViewState()
    init() {
        bridge.observe { [weak self] json in
            Task { @MainActor in
                do { self?.state = try JSONDecoder().decode(RhythmetaViewState.self, from: Data(json.utf8)) }
                catch { self?.loginError = error.localizedDescription }
            }
        }
        bridge.observeCommunity(directory: URL.applicationSupportDirectory.appending(path: "community").path) { [weak self] json in
            Task { @MainActor in
                do { self?.community = try JSONDecoder().decode(CommunityViewState.self, from: Data(json.utf8)) }
                catch { self?.loginError = error.localizedDescription }
            }
        }
        bridge.start()
    }
}

struct RhythmetaAccountView: View {
    @Environment(\.webAuthenticationSession) private var webAuthenticationSession
    @Bindable var store: RhythmetaAccountStore
    @State private var restoring: RhythmetaViewState.Backup?
    @State private var authenticating = false

    var body: some View {
        List {
            Section(tr("Rhythmeta 账号")) {
                if let user = store.state.user {
                    LabeledContent(tr("账号"), value: user.handle)
                    LabeledContent(tr("邮箱"), value: user.email)
                    Button(tr("退出账号"), role: .destructive) { store.bridge.logout() }
                } else {
                    Button(tr("登录 / 注册")) {
                        Task {
                            authenticating = true
                            defer { authenticating = false }
                            do {
                                guard let url = URL(string: store.bridge.loginUrl()) else { return }
                                let callback = try await webAuthenticationSession.authenticate(using: url, callback: .customScheme("chunithmd"), additionalHeaderFields: [:])
                                store.bridge.handleCallback(url: callback.absoluteString)
                            } catch { store.loginError = error.localizedDescription }
                        }
                    }
                }
            }
            if let error = store.state.error ?? store.loginError {
                Section { Text(error).foregroundStyle(.red) }
            }
            if store.state.user != nil {
                Section {
                    Button(tr("立即备份")) { store.bridge.backup(deviceName: "chunithmd iOS") }
                    Button(tr("刷新备份列表")) { store.bridge.refresh() }
                } footer: {
                    Text(tr("保留最近三份备份。恢复会替换全部本地档案、成绩、收藏和设置。恢复完成后，各页面会重新加载个人数据。"))
                }
                Section(tr("云端备份")) {
                    ForEach(store.state.backups) { backup in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(backup.committedAt)
                            Text(tr("{0} · {1} 个档案 · {2} KB", backup.deviceName, backup.profileCount, backup.size / 1024)).font(.caption).foregroundStyle(.secondary)
                            Button(tr("恢复此备份")) { restoring = backup }
                        }
                    }
                }
            }
        }
        .navigationTitle(tr("Rhythmeta 账号"))
        .disabled(store.state.busy || authenticating || !store.state.ready)
        .overlay { if store.state.busy { ProgressView(tr("正在处理备份…")).padding().background(.regularMaterial, in: .rect(cornerRadius: 14)) } }
        .interactiveDismissDisabled(store.state.busy)
        .confirmationDialog(tr("替换全部本地数据？"), isPresented: Binding(get: { restoring != nil }, set: { if !$0 { restoring = nil } }), titleVisibility: .visible) {
            if let backup = restoring { Button(tr("替换并恢复"), role: .destructive) { store.bridge.restore(id: backup.id); restoring = nil } }
            Button(tr("取消"), role: .cancel) { restoring = nil }
        } message: { Text(tr("开始前会保存回滚副本，失败时自动恢复。")) }
    }
}
