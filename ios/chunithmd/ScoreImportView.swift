import Shared
import SwiftUI

struct ScoreImportView: View {
    var provider: String? = nil
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var store = ScoreImportStore()
    @State private var code = ""
    @State private var showOtogame = false
    var body: some View {
        Form {
            Section { Text(tr("导入到档案：{0}", personal.snapshot.activeProfile?.name ?? tr("我的档案"))).font(.headline); Text(tr("已有成绩会保留，重复记录会自动跳过。")).font(.subheadline).foregroundStyle(.secondary) }
            ForEach(provider.map { [$0] } ?? ["fish", "lxns", "otogame"], id: \.self) { provider in
                let state = store.states[provider] ?? ScoreImportStore.Provider()
                Section(provider == "fish" ? tr("水鱼查分器") : provider == "lxns" ? tr("落雪咖啡屋") : "Otogame") {
                    if provider == "otogame" && personal.snapshot.activeProfile?.server != "jp" { Text(tr("需要启用一个日服档案。")).foregroundStyle(.secondary) }
                    else {
                        if state.connected {
                            Button(tr("导入成绩")) { store.bridge?.importScores(provider: provider) }.disabled(state.busy)
                            Button(tr("断开连接"), role: .destructive) { store.bridge?.disconnect(provider: provider) }.disabled(state.busy)
                        } else {
                            Button(provider == "otogame" ? tr("登录 Otogame") : tr("授权并导入")) {
                                if provider == "otogame" { showOtogame = true } else { store.bridge?.authorize(provider: provider) }
                            }.disabled(state.busy)
                        }
                        if let urlString = state.url, let url = URL(string: urlString) { Link(tr("打开授权页面"), destination: url) }
                        if let code = state.code { LabeledContent(tr("授权码"), value: code).textSelection(.enabled) }
                        if provider == "lxns", state.url != nil {
                            TextField(tr("粘贴浏览器中的授权码"), text: $code).textInputAutocapitalization(.never).autocorrectionDisabled()
                            Button(tr("确认授权并导入")) { store.bridge?.exchange(code: code); code = "" }.disabled(state.busy || code.isEmpty)
                        }
                        if state.busy { ProgressView(tr("正在处理…")); Button(tr("取消")) { store.bridge?.cancel() } }
                        if let error = state.error { Text(error).foregroundStyle(.red) }
                        if let result = state.result { Text(result).foregroundStyle(.secondary) }
                    }
                }
            }
            if catalog.bundle == nil { Text(tr("请先在静态数据中下载歌曲目录。")).foregroundStyle(.secondary) }
        }.navigationTitle(tr("导入成绩"))
            .task(id: personal.snapshot.activeProfile?.id) {
                guard let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else { return }
                store.start(bundle: bundle, profile: profile)
            }
            .onDisappear { store.close(); personal.reload(catalog: catalog) }
            .sheet(isPresented: $showOtogame) {
                OtogameLoginView { header in
                    guard let profile = personal.snapshot.activeProfile else { return }
                    store.bridge?.captureOtogame(profileId: profile.id, header: header)
                    showOtogame = false
                }
            }
    }
}
