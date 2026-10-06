import Shared
import SwiftUI

struct CollectionPickerView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var selected = Set<String>()

    var body: some View {
        NavigationStack {
            List {
                if personal.snapshot.collections.isEmpty {
                    ContentUnavailableView(tr("还没有收藏夹"), systemImage: "rectangle.stack",
                                           description: Text(tr("前往主页的「收藏夹」新建一个，再回来收下这张谱面。")))
                        .listRowBackground(Color.clear)
                }
                ForEach(personal.snapshot.collections) { collection in
                    let isSelected = selected.contains(collection.id)
                    let items = personal.snapshot.collectionItems.filter { $0.collectionId == collection.id }
                    let containsChart = items.contains(where: matches)
                    Button {
                        if isSelected { selected.remove(collection.id) } else { selected.insert(collection.id) }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: isSelected ? "checkmark.square.fill" : "square")
                                .font(.title3).foregroundStyle(isSelected ? Color.accentColor : .secondary).frame(width: 24)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(collection.name).foregroundStyle(.primary)
                                Text(tr("{0} 张谱面", items.count + (isSelected ? 1 : 0) - (containsChart ? 1 : 0)))
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer(minLength: 0)
                        }.contentShape(.rect)
                    }
                    .buttonStyle(.plain).accessibilityIdentifier("collection-picker-" + collection.id)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
            .navigationTitle(tr("加入收藏夹")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("取消"), systemImage: "xmark") { dismiss() }.labelStyle(.iconOnly).tint(.primary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("完成"), systemImage: "checkmark", action: save).labelStyle(.iconOnly).tint(.primary)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear { selected = Set(personal.snapshot.collectionItems.filter(matches).map(\.collectionId)) }
    }

    private func matches(_ item: PersonalSnapshot.Item) -> Bool {
        item.songId == song.id && item.chartType.lowercased() == sheet.type.lowercased() && item.difficulty.lowercased() == sheet.difficulty.lowercased()
    }

    private func save() {
        do {
            try personal.bridge.setChartCollections(songId: song.id, type: sheet.type, difficulty: sheet.difficulty, selectedIds: Array(selected))
            personal.reload(catalog: catalog); dismiss()
        } catch { personal.error = error.localizedDescription }
    }
}
