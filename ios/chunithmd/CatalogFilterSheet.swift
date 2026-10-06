import SwiftUI
import Shared

struct CatalogFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var settings: CatalogFilterState
    let categories: [String]
    let versions: [String]
    let difficulties: [String]

    init(store: CatalogStore) {
        _settings = Binding(get: { store.filters }, set: { store.filters = $0 })
        categories = store.categories
        versions = store.versions
        difficulties = store.difficulties
    }

    init(settings: Binding<CatalogFilterState>, categories: [String], versions: [String], difficulties: [String]) {
        _settings = settings
        self.categories = categories
        self.versions = versions
        self.difficulties = difficulties
    }
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    FilterSection(title: tr("快速筛选")) {
                        Toggle(isOn: $settings.favoritesOnly) { Label(tr("仅显示喜爱歌曲"), systemImage: "star") }
                        Toggle(isOn: $settings.hideDeleted) { Label(tr("隐藏删除曲"), systemImage: "eye.slash") }
                        Toggle(isOn: $settings.playableOnly) { Label(tr("仅显示可玩歌曲"), systemImage: "play.circle") }
                    }.font(.system(size: 14, weight: .semibold))
                    FilterSection(title: tr("难度与定数")) {
                        FilterChoices(options: difficulties, selection: $settings.difficulties, difficulty: true)
                        Divider()
                        HStack {
                            Text(tr("定数范围")).font(.system(size: 11, weight: .bold)).foregroundStyle(.secondary)
                            Spacer()
                            Text("\(settings.minLevel.formatted(.number.precision(.fractionLength(1)))) – \(settings.maxLevel.formatted(.number.precision(.fractionLength(1))))")
                                .font(.subheadline.bold().monospacedDigit())
                        }
                        LevelRangeSlider(lower: $settings.minLevel, upper: $settings.maxLevel, active: !settings.difficulties.isEmpty)
                        Text(tr("选择难度后，按该难度的定数范围筛选。")).font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                    FilterSection(title: tr("分类")) { FilterChoices(options: categories, selection: $settings.categories) }
                    FilterSection(title: tr("版本")) {
                        FilterChoices(options: Array(versions.reversed()), selection: $settings.versions,
                                      displayValue: { CatalogVersionFormatter.shared.badge(version: $0) })
                    }
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
        settings = CatalogFilterState()
    }
}
