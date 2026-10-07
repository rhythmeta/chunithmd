import PhotosUI
import SwiftUI

struct ScannerView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var store = ScannerStore()
    @State private var models = ScannerModelController()
    @State private var photo: PhotosPickerItem?

    var body: some View {
        Form {
            ScannerModelDownloadView(models: models)
            Section {
                Text(tr("选择成绩图，在设备上识别并核对后保存。"))
                PhotosPicker(selection: $photo, matching: .images) {
                    Label(tr("选择成绩图"), systemImage: "photo")
                }.disabled(!models.state.usable || catalog.bundle == nil || personal.snapshot.activeProfile == nil || store.busy)
                if catalog.bundle == nil || personal.snapshot.activeProfile == nil {
                    Text(tr("请先加载曲库并选择玩家档案。"))
                }
            }
            if store.busy { Section { ProgressView(tr("正在识别…")) } }
            if let error = store.error { Section { Text(error).foregroundStyle(.red) } }
            if let preview = store.preview {
                Section { Image(uiImage: preview).resizable().scaledToFit().frame(maxHeight: 280).accessibilityLabel(tr("成绩图")) }
                Section(tr("谱面")) {
                    TextField(tr("曲名"), text: $store.title)
                    TextField(tr("难度"), text: $store.difficulty)
                    TextField(tr("等级 / WE 属性"), text: $store.level)
                    Button(tr("匹配谱面")) { if let bundle = catalog.bundle { store.match(catalog: bundle, region: personal.snapshot.activeProfile?.server ?? "jp") } }
                    if store.candidates.isEmpty { Text(tr("未找到匹配谱面，请修改曲名、难度或属性后重试。")) }
                    ForEach(store.candidates) { candidate in
                        Button {
                            store.selectedKey = candidate.id
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(candidate.title)
                                    Text(candidate.difficulty.uppercased() + " · " + candidate.level).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if store.selectedKey == candidate.id { Image(systemName: "checkmark") }
                            }
                        }.tint(.primary)
                    }
                }
                Section(tr("成绩")) {
                    TextField(tr("分数"), text: $store.score).keyboardType(.numberPad)
                    Picker(tr("CLEAR 状态"), selection: $store.clear) {
                        Text(tr("无")).tag(""); Text(tr("CLEAR")).tag("clear"); Text(tr("FAILED")).tag("failed")
                    }
                    Picker(tr("COMBO 状态"), selection: $store.combo) {
                        Text(tr("无")).tag(""); Text("FC").tag("fullcombo"); Text("AJ").tag("alljustice"); Text("AJC").tag("alljusticecritical")
                    }
                    Text(store.rawStatuses).font(.caption).foregroundStyle(.secondary)
                    Text(tr("灰色未点亮的 COMBO 字样也可能被读出，请对照图片选择状态。")).font(.caption)
                }
                Section {
                    Button(store.saved ? tr("已保存") : tr("确认并保存")) { store.save(catalog: catalog, personal: personal) }
                        .disabled(store.selectedKey.isEmpty || store.busy || store.saved)
                }
            }
        }
        .navigationTitle(tr("扫描"))
        .task { models.start() }
        .onChange(of: personal.snapshot.activeProfile?.id) { photo = nil; store.reset() }
        .onChange(of: personal.snapshot.activeProfile?.server) { photo = nil; store.reset() }
        .task(id: photo) {
            guard let photo, let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else { return }
            do {
                guard let data = try await photo.loadTransferable(type: Data.self) else { store.error = tr("识别失败"); return }
                try Task.checkCancellation()
                await store.recognize(data: data, catalog: bundle, region: profile.server, files: try models.files())
            } catch is CancellationError { }
            catch { if !Task.isCancelled { store.error = error.localizedDescription } }
        }
    }
}
