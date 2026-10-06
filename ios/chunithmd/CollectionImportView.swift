import SwiftUI
import Shared

struct CollectionImportView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var text = ""
    @State private var preview: CollectionExport?
    @State private var error: String?

    init(initialText: String = "") {
        _text = State(initialValue: initialText)
    }
    var body: some View {
        NavigationStack {
            Form {
                Section(tr("收藏夹链接或分享码")) {
                    TextField(tr("请输入 chunithmd 收藏夹链接或 CHMD1 分享码"), text: $text, axis: .vertical).lineLimit(3...6).textInputAutocapitalization(.never).autocorrectionDisabled()
                    Button(tr("预览收藏夹"), action: decode).disabled(text.isEmpty)
                }
                if let preview {
                    Section(preview.name) {
                        Text(tr("{0} 张谱面", preview.entries.count))
                        Button(tr("导入收藏夹"), action: save).buttonStyle(.borderedProminent)
                    }
                }
                if let error { Text(error).foregroundStyle(.red) }
            }.navigationTitle(tr("导入收藏夹")).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() } } }
        }.onAppear { if !text.isEmpty { decode() } }
            .onChange(of: text) { preview = nil }
    }
    private func decode() {
        do { preview = try PortableCollectionCodec.shared.decode(text: text); error = nil }
        catch { self.error = error.localizedDescription }
    }
    private func save() {
        do { _ = try personal.bridge.importCollection(text: text); personal.reload(catalog: catalog); dismiss() }
        catch { self.error = error.localizedDescription }
    }
}
