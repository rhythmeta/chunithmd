import SwiftUI

struct CatalogFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Bindable var store: CatalogStore
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    FilterSection(title: tr("快捷筛选")) {
                        Toggle(isOn: $store.favoritesOnly) { Label(tr("仅显示收藏"), systemImage: "star") }
                        Toggle(isOn: $store.hideDeleted) { Label(tr("隐藏已删除曲目"), systemImage: "eye.slash") }
                        Toggle(isOn: $store.playableOnly) { Label(tr("仅显示当前服务器可玩"), systemImage: "play.circle") }
                    }.font(.system(size: 14, weight: .semibold))
                    FilterSection(title: tr("难度与定数")) {
                        FilterChoices(options: store.difficulties, selection: $store.selectedDifficulties, difficulty: true)
                        Divider()
                        HStack {
                            Text(tr("定数范围")).font(.system(size: 11, weight: .bold)).foregroundStyle(.secondary)
                            Spacer()
                            Text("\(store.minLevel.formatted(.number.precision(.fractionLength(1)))) – \(store.maxLevel.formatted(.number.precision(.fractionLength(1))))")
                                .font(.subheadline.bold().monospacedDigit())
                        }
                        LevelRangeSlider(lower: $store.minLevel, upper: $store.maxLevel, active: !store.selectedDifficulties.isEmpty)
                        Text(tr("选择难度后，按该难度的定数范围筛选。")).font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                    FilterSection(title: tr("分类")) { FilterChoices(options: store.categories, selection: $store.selectedCategories) }
                    FilterSection(title: tr("版本")) { FilterChoices(options: Array(store.versions.reversed()), selection: $store.selectedVersions) }
                }.padding(16)
            }
            .background(AppTheme.page).navigationTitle(tr("筛选歌曲")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("重置"), systemImage: "arrow.counterclockwise", action: reset)
                        .labelStyle(.iconOnly).tint(.primary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("完成"), systemImage: "checkmark") { dismiss() }
                        .labelStyle(.iconOnly).tint(.primary)
                }
            }
        }
    }
    private func reset() {
        store.selectedCategories = []; store.selectedVersions = []; store.selectedDifficulties = []
        store.playableOnly = false; store.hideDeleted = false; store.favoritesOnly = false
        store.minLevel = 1; store.maxLevel = 16
    }
}
