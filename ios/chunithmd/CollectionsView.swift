import Shared
import SwiftUI

struct CollectionsView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var adding = false
    @State private var name = ""
    @State private var deleting: PersonalSnapshot.Collection?
    @State private var shareURLs: [String: URL] = [:]

    var body: some View {
        List {
            ForEach(personal.snapshot.collections) { collection in
                let items = personal.snapshot.collectionItems.filter { $0.collectionId == collection.id }
                NavigationLink { CollectionSongsView(collectionID: collection.id, title: collection.name) } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 12) {
                            Image(systemName: "rectangle.stack").foregroundStyle(.secondary)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(collection.name)
                                Text(tr("{0} 张谱面", items.count)).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        if !items.isEmpty { CollectionPreviewRow(items: items) }
                    }
                }
                .accessibilityIdentifier("collection-" + collection.id)
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(tr("删除"), systemImage: "trash", role: .destructive) { deleting = collection }.tint(.red)
                    if let url = shareURLs[collection.id] {
                        ShareLink(item: url) { Label(tr("分享收藏夹"), systemImage: "square.and.arrow.up") }.tint(.blue)
                    }
                }
                .contextMenu {
                    if let url = shareURLs[collection.id] {
                        ShareLink(item: url) { Label(tr("分享收藏夹"), systemImage: "square.and.arrow.up") }
                    }
                    Button(tr("删除收藏夹"), systemImage: "trash", role: .destructive) { deleting = collection }
                }
            }
            if personal.snapshot.collections.isEmpty {
                ContentUnavailableView(tr("从第一个收藏夹开始"), systemImage: "rectangle.stack",
                                       description: Text(tr("创建收藏夹，整理练习曲目。")))
                    .listRowBackground(Color.clear)
            }
        }
        .navigationTitle(tr("收藏夹"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(tr("从剪贴板导入"), systemImage: "doc.on.clipboard") {
                        let text = UIPasteboard.general.string ?? ""
                        personal.perform(catalog: catalog) {
                            _ = try personal.bridge.importCollection(text: text)
                        }
                    }
                    Button(tr("新建收藏夹"), systemImage: "plus") { name = ""; adding = true }
                } label: { Label(tr("新建收藏夹"), systemImage: "plus") }
                    .tint(.primary).accessibilityIdentifier("collections-actions")
            }
        }
        .task(id: personal.revision) {
            shareURLs = Dictionary(uniqueKeysWithValues: personal.snapshot.collections.compactMap { collection in
                guard let text = try? personal.bridge.collectionLink(id: collection.id), let url = URL(string: text) else { return nil }
                return (collection.id, url)
            })
        }
        .alert(tr("新建收藏夹"), isPresented: $adding) {
            TextField(tr("收藏夹名称"), text: $name)
            Button(tr("取消"), role: .cancel) {}
            Button(tr("创建")) { personal.perform(catalog: catalog) { try personal.bridge.saveCollection(id: nil, name: name) } }
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .confirmationDialog(tr("删除收藏夹？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            if let deleting { Button(tr("删除"), role: .destructive) { personal.perform(catalog: catalog) { try personal.bridge.deleteCollection(id: deleting.id) }; self.deleting = nil }.accessibilityIdentifier("collection-delete-confirm") }
        }
    }
}
