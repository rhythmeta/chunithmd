import Shared
import SwiftUI

struct ScoreImportView: View {
    let provider: String
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.openURL) private var openURL
    @State private var store = ScoreImportStore()
    @State private var code = ""
    @State private var showOtogame = false
    @State private var awaitingBrowser = false

    private var state: ScoreImportStore.Provider { store.states[provider] ?? .init() }
    private var title: String {
        switch provider {
        case "fish": tr("从水鱼查分器导入")
        case "lxns": tr("从落雪咖啡屋导入")
        default: tr("从 Otogame 导入")
        }
    }
    private struct SessionID: Hashable {
        let profile: String?
        let server: String?
        let bundle: ObjectIdentifier?
    }
    private var sessionID: SessionID {
        SessionID(profile: personal.snapshot.activeProfile?.id,
                  server: personal.snapshot.activeProfile?.server,
                  bundle: catalog.bundle.map(ObjectIdentifier.init))
    }

    var body: some View {
        List {
            Group {
                switch provider {
                case "fish":
                    DivingFishImportSections(state: state, authorize: authorize, sync: synchronize) {
                        store.bridge?.disconnect(provider: provider)
                    }
                case "lxns":
                    LxnsImportSections(state: state, code: $code, authorize: authorize, sync: synchronize,
                                       disconnect: { store.bridge?.disconnect(provider: provider) }) {
                        store.bridge?.exchange(code: code)
                        code = ""
                    }
                default:
                    otogameSections
                }
            }
            .disabled(store.bridge == nil)
            ScoreImportStatusSection(state: state, error: store.error) {
                awaitingBrowser = false
                store.bridge?.cancel()
            }
            if catalog.bundle == nil {
                Section { Text(tr("请先在静态数据中下载歌曲目录。")).foregroundStyle(.secondary) }
            }
        }
        .listStyle(.insetGrouped)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $showOtogame) {
            if let profileID = personal.snapshot.activeProfile?.id {
                OtogameLoginView(connected: state.connected) { header in
                    store.bridge?.captureOtogame(profileId: profileID, header: header)
                }
            }
        }
        .task(id: sessionID) {
            guard let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else {
                store.close()
                return
            }
            if store.start(bundle: bundle, profile: profile) {
                code = ""
                awaitingBrowser = false
                showOtogame = false
            }
        }
        .onChange(of: state.url) { _, value in
            guard awaitingBrowser, let value, let url = URL(string: value) else { return }
            awaitingBrowser = false
            openURL(url)
        }
        .onChange(of: state.error) { _, error in
            if error != nil { awaitingBrowser = false }
        }
        .onDisappear {
            if !showOtogame { store.close() }
            personal.reload(catalog: catalog)
        }
    }

    private var otogameSections: some View {
        Group {
            Section {
                Label {
                    VStack(alignment: .leading) {
                        Text(state.connected ? tr("Otogame 会话已就绪") : tr("请先登录 Otogame"))
                        if let profile = personal.snapshot.activeProfile {
                            Text(tr("当前档案：{0}", profile.name)).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                } icon: {
                    Image(systemName: state.connected ? "checkmark.circle.fill" : "person.crop.circle.badge.exclamationmark")
                        .foregroundStyle(state.connected ? .green : .secondary)
                }
            } footer: {
                Text(state.eligible ? tr("仅导入最近四页游玩记录") : tr("需要启用一个日服档案。"))
            }
            Section {
                Button { showOtogame = true } label: {
                    HStack {
                        Label(tr("登录 Otogame"), systemImage: "person.crop.circle.badge.checkmark")
                        Spacer()
                        Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                    }
                }
                .tint(.primary)
                .disabled(!state.eligible || state.busy)
                .accessibilityIdentifier("import-otogame-login")
                Button(tr("同步游玩记录"), systemImage: "arrow.triangle.2.circlepath", action: synchronize)
                    .disabled(!state.eligible || !state.connected || state.busy)
                    .accessibilityIdentifier("import-sync")
            }
        }
    }

    private func authorize() {
        awaitingBrowser = true
        store.bridge?.authorize(provider: provider)
    }

    private func synchronize() { store.bridge?.importScores(provider: provider) }
}
