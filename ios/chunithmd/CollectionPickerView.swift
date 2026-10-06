import Shared
import SwiftUI

struct CollectionPickerView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var name = ""
    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(personal.snapshot.collections) { collection in
                        Button { personal.perform(catalog: catalog) { try personal.bridge.toggleCollectionSong(collectionId: collection.id, songId: song.id, type: sheet.type, difficulty: sheet.difficulty) } } label: {
                            HStack {
                                Label(collection.name, systemImage: "folder")
                                Spacer()
                                if personal.snapshot.collectionItems.contains(where: { $0.collectionId == collection.id && $0.songId == song.id && $0.chartType == sheet.type && $0.difficulty == sheet.difficulty }) { Image(systemName: "checkmark") }
                            }
                        }
                    }
                }
                Section(tr("新建收藏夹")) {
                    TextField(tr("收藏夹名称"), text: $name)
                    Button(tr("创建")) { personal.perform(catalog: catalog) { try personal.bridge.saveCollection(id: nil, name: name) }; name = "" }.disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }.navigationTitle(tr("加入收藏夹")).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button(tr("完成")) { dismiss() } } }
        }.presentationDetents([.medium, .large])
    }
}
