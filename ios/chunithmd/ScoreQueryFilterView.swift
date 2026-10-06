import Shared
import SwiftUI

struct ScoreQueryFilterView: View {
    @Binding var filters: ScoreQueryFilters
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        NavigationStack {
            Form {
                Section(tr("难度")) {
                    FilterChoices(options: difficultyNames, selection: $filters.difficulties, difficulty: true)
                }.listRowBackground(cardBackground)
                Section(tr("段位")) {
                    FilterChoices(options: ChunithmScoreRules.shared.rankThresholds.reversed().map(\.rank),
                                  selection: $filters.ranks, tint: { AnyShapeStyle(scoreRankColor($0)) })
                }.listRowBackground(cardBackground)
                Section(tr("Full Combo")) {
                    FilterChoices(options: FullComboType.entries.map(\.displayName), selection: $filters.fullCombos,
                                  displayValue: { tr($0) }, tint: { AnyShapeStyle(scoreQueryComboColor($0)) })
                }.listRowBackground(cardBackground)
                Section(tr("Full Chain")) {
                    FilterChoices(options: FullChainType.entries.map(\.displayName), selection: $filters.fullChains,
                                  displayValue: { tr($0) }, tint: { AnyShapeStyle(scoreQueryChainColor($0)) })
                }.listRowBackground(cardBackground)
            }
            .navigationTitle(tr("筛选"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("重置筛选"), systemImage: "arrow.counterclockwise") { filters = ScoreQueryFilters() }
                        .disabled(filters.shared.isEmpty)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("完成"), systemImage: "checkmark") { dismiss() }
                }
            }
            .labelStyle(.iconOnly)
            .tint(.primary)
        }
    }

    private var cardBackground: Color { .gray.opacity(colorScheme == .dark ? 0.22 : 0.08) }
}
