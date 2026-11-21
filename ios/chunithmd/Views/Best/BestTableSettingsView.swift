import Shared
import SwiftUI

struct BestTableSettingsView: View {
    @Binding var filters: BestTableFilterState
    @Binding var theoretical: Bool
    @Binding var selectedVersion: String?
    @Binding var bestCount: Int
    @Binding var newCount: Int

    let serverVersion: String?
    let versions: [String]
    let difficulties: [String]
    let categories: [String]
    let ranks: [String]
    let onCommitCapacity: () -> Void
    let onReset: () -> Void

    @Environment(\.dismiss) private var dismiss
    private let clearOptions = ["catastrophy", "absolute", "brave", "hard", "clear", "failed"]
    private let comboOptions = ["alljusticecritical", "alljustice", "fullcombo"]
    private let chainOptions = ["fullchain2", "fullchain"]

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(tr("理论 B50"), isOn: $theoretical)

                    LabeledContent(tr("游戏版本")) {
                        Menu {
                            Button {
                                selectedVersion = nil
                            } label: {
                                versionLabel(nil)
                            }
                            Divider()
                            ForEach(versions.reversed(), id: \.self) { version in
                                Button {
                                    selectedVersion = version
                                } label: {
                                    versionLabel(version)
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Text(selectedVersionLabel)
                                Image(systemName: "chevron.up.chevron.down").font(.caption2)
                            }
                            .contentShape(.rect)
                        }
                        .tint(.secondary)
                    }
                } header: {
                    Text(tr("Best 表"))
                } footer: {
                    Text(tr("显示当前版本的理论最高 Rating"))
                }

                if !theoretical {
                    Section {
                        BestTableCapacitySlider(
                            label: tr("旧曲"),
                            value: $bestCount,
                            snapValue: 30,
                            onCommit: onCommitCapacity
                        )
                        BestTableCapacitySlider(
                            label: tr("新曲"),
                            value: $newCount,
                            snapValue: 20,
                            onCommit: onCommitCapacity
                        )
                    } header: {
                        Text(tr("容量设置"))
                    } footer: {
                        Text(tr("当前总计 {0} 首", bestCount + newCount))
                    }

                    filterSections
                }
            }
            .navigationTitle(tr("Best 表设置"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        onReset()
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                    }
                    .accessibilityLabel(tr("重置"))
                    .tint(.black)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "checkmark")
                            .fontWeight(.semibold)
                    }
                    .accessibilityLabel(tr("完成"))
                    .tint(.black)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    @ViewBuilder
    private var filterSections: some View {
        Section(tr("难度等级")) {
            FilterChoices(
                options: difficulties,
                selection: $filters.difficulties,
                displayValue: { $0.uppercased() },
                tint: { AnyShapeStyle(difficultyStyle($0, vertical: false)) }
            )
        }
        if !categories.isEmpty {
            Section(tr("分类")) {
                FilterChoices(options: categories, selection: $filters.categories)
            }
        }
        Section(tr("Rank")) {
            FilterChoices(
                options: ranks,
                selection: $filters.ranks,
                tint: { AnyShapeStyle(scoreRankColor($0)) }
            )
        }
        Section(tr("CLEAR")) {
            FilterChoices(
                options: clearOptions,
                selection: $filters.clears,
                displayValue: statusName,
                tint: { AnyShapeStyle(scoreQueryClearColor($0)) }
            )
        }
        Section(tr("FULL COMBO")) {
            FilterChoices(
                options: comboOptions,
                selection: $filters.combos,
                displayValue: statusName,
                tint: { AnyShapeStyle(scoreQueryComboColor($0)) }
            )
        }
        Section(tr("FULL CHAIN")) {
            FilterChoices(
                options: chainOptions,
                selection: $filters.chains,
                displayValue: statusName,
                tint: { AnyShapeStyle(scoreQueryChainColor($0)) }
            )
        }
    }

    private var selectedVersionLabel: String {
        if let selectedVersion {
            return CatalogVersionFormatter.shared.badge(version: selectedVersion)
        }
        if let serverVersion {
            return "\(tr("自动")) (\(CatalogVersionFormatter.shared.badge(version: serverVersion)))"
        }
        return tr("自动")
    }

    private func versionLabel(_ version: String?) -> some View {
        HStack {
            Text(version.map { CatalogVersionFormatter.shared.badge(version: $0) } ?? tr("自动"))
            if version == selectedVersion { Image(systemName: "checkmark") }
        }
    }

    private func statusName(_ value: String) -> String {
        switch value.lowercased() {
        case "alljusticecritical": return "AJC"
        case "alljustice": return "AJ"
        case "fullcombo": return "FC"
        case "fullchain": return tr("铂 FC")
        case "fullchain2": return tr("金 FC")
        default:
            if let name = ClearType.companion.displayName(value: value) { return tr(name) }
            return value
        }
    }
}

private struct BestTableCapacitySlider: View {
    let label: String
    @Binding var value: Int
    let snapValue: Int
    let onCommit: () -> Void
    @State private var snapTick = 0
    @State private var wasSnapped = false

    private var sliderBinding: Binding<Double> {
        Binding(
            get: { Double(value) },
            set: { raw in
                let rounded = Swift.min(Swift.max(Int(raw.rounded()), 1), 99)
                let snapped = abs(rounded - snapValue) <= 1 ? snapValue : rounded
                if snapped != value { value = snapped }
                let isSnapped = snapped == snapValue
                if isSnapped, !wasSnapped { snapTick += 1 }
                wasSnapped = isSnapped
            }
        )
    }

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(label)
                Spacer()
                Text(tr("{0} 首", value))
                    .font(.body.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Slider(value: sliderBinding, in: 1...99, step: 1) { editing in
                if !editing { onCommit() }
            }
            .sensoryFeedback(.selection, trigger: snapTick)
        }
        .padding(.vertical, 2)
    }
}
