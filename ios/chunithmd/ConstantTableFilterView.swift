import Shared
import SwiftUI

struct ConstantTableFilterView: View {
    @Binding var settings: CatalogFilterState
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    FilterSection(title: tr("快速筛选")) {
                        Toggle(isOn: $settings.favoritesOnly) { Label(tr("仅显示喜爱歌曲"), systemImage: "star") }
                            .font(.system(size: 14, weight: .semibold))
                    }
                    FilterSection(title: tr("分类")) { FilterChoices(options: catalog.categories, selection: $settings.categories) }
                    FilterSection(title: tr("版本")) {
                        FilterChoices(options: Array(catalog.versions.reversed()), selection: $settings.versions,
                            displayValue: { CatalogVersionFormatter.shared.badge(version: $0) })
                    }
                }.padding(16)
            }
            .background(AppTheme.page).navigationTitle(tr("筛选歌曲")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("重置"), systemImage: "arrow.counterclockwise") { settings = CatalogFilterState() }
                        .labelStyle(.iconOnly).tint(.primary).disabled(!settings.isActive)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("完成"), systemImage: "checkmark") { dismiss() }.labelStyle(.iconOnly).tint(.primary)
                }
            }
        }
    }
}
