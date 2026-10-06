import Shared
import SwiftUI

struct CollectionsView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var importing = false
    @State private var adding = false
    @State private var name = ""
    @State private var deleting: PersonalSnapshot.Collection?
    var body: some View {
        List {
            NavigationLink { CollectionSongsView(collectionID: nil, title: tr("喜欢的歌曲")) } label: { Label(tr("喜欢的歌曲"), systemImage: "heart.fill").foregroundStyle(.pink) }
            Section(tr("我的收藏夹")) {
                ForEach(personal.snapshot.collections) { collection in
                    NavigationLink { CollectionSongsView(collectionID: collection.id, title: collection.name) } label: {
                        VStack(alignment: .leading, spacing: 8) {
                            let items = personal.snapshot.collectionItems.filter { $0.collectionId == collection.id }
                            HStack(spacing: 12) {
                                Image(systemName: "rectangle.stack").foregroundStyle(.secondary)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(collection.name)
                                    Text(tr("{0} 个谱面", items.count)).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            HStack(spacing: 6) {
                                ForEach(Array(items.prefix(4))) { item in
                                    if let song = catalog.allSongs.first(where: { $0.id == item.songId }) {
                                        JacketImage(url: catalog.jacketURL(for: song.imageName))
                                            .frame(width: 52, height: 52).clipShape(.rect(cornerRadius: 8))
                                    }
                                }
                            }
                        }.padding(.vertical, 4)
                    }
                        .swipeActions { Button(tr("删除"), role: .destructive) { deleting = collection } }
                }
                if personal.snapshot.collections.isEmpty { Text(tr("创建收藏夹，整理练习曲目。")).foregroundStyle(.secondary) }
            }
        }
        .navigationTitle(tr("收藏夹"))
        .toolbar { Button(tr("导入收藏夹"), systemImage: "square.and.arrow.down") { importing = true }; Button(tr("新建收藏夹"), systemImage: "folder.badge.plus") { name = ""; adding = true } }
        .sheet(isPresented: $importing) { CollectionImportView() }
        .alert(tr("新建收藏夹"), isPresented: $adding) {
            TextField(tr("名称"), text: $name)
            Button(tr("取消"), role: .cancel) {}
            Button(tr("创建")) { personal.perform(catalog: catalog) { try personal.bridge.saveCollection(id: nil, name: name) } }
        }
        .confirmationDialog(tr("删除收藏夹？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            if let deleting { Button(tr("删除"), role: .destructive) { personal.perform(catalog: catalog) { try personal.bridge.deleteCollection(id: deleting.id) }; self.deleting = nil } }
        }
    }
}
