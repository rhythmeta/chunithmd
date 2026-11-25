import Shared
import SwiftUI

struct BestTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.colorScheme) private var colorScheme

    @State private var response: BestTableResponse?
    @State private var entries: [BestTableEntry] = []
    @State private var selectedVersion: String?
    @State private var theoretical = false
    @State private var filters = BestTableFilterState()
    @State private var bestCount = 30
    @State private var newCount = 20
    @State private var showSettings = false
    @State private var sharing = false

    private var filteredEntries: [BestTableEntry] {
        BestTableCalculatorKt.filterBestTableEntries(
            entries: entries,
            difficultyFilter: filters.difficulties.sorted(),
            categoryFilter: filters.categories.sorted(),
            rankFilter: filters.ranks.sorted(),
            clearFilter: filters.clears.sorted(),
            comboFilter: filters.combos.sorted(),
            chainFilter: filters.chains.sorted()
        )
    }

    private var summary: PlayerRatingSummary? {
        guard response != nil else { return nil }
        let chartEntries = filteredEntries.map { entry in
            RatingChartEntry(chartId: entry.chartId, songId: entry.songId, rating: entry.rating, isNew: entry.isNew)
        }
        return RatingCalculatorKt.calculatePlayerRating(
            entries: chartEntries,
            bestSlotCount: Int32(bestCount),
            newSlotCount: Int32(newCount)
        )
    }

    private var selectedBestEntries: [BestTableEntry] {
        guard let summary else { return [] }
        let byID = Dictionary(uniqueKeysWithValues: filteredEntries.map { ($0.chartId, $0) })
        return summary.best30.compactMap { byID[$0.chartId] }
    }

    private var selectedNewEntries: [BestTableEntry] {
        guard let summary else { return [] }
        let byID = Dictionary(uniqueKeysWithValues: filteredEntries.map { ($0.chartId, $0) })
        return summary.new20.compactMap { byID[$0.chartId] }
    }

    private var settingsActive: Bool {
        theoretical || selectedVersion != nil || !filters.isEmpty || bestCount != 30 || newCount != 20
    }

    private var difficultyOptions: [String] {
        let present = Set(entries.map { $0.difficulty.lowercased() })
        let order = ["basic", "advanced", "expert", "master", "ultima"]
        return order + present.subtracting(order).sorted()
    }

    private var rankOptions: [String] {
        let order = ["SSS+", "SSS", "SS+", "SS", "S+", "S", "AAA", "AA", "A", "BBB", "BB", "B", "C", "D"]
        let present = Set(entries.map { $0.rank.uppercased() })
        return order.filter(present.contains) + present.subtracting(order).sorted()
    }

    private var categoryOptions: [String] {
        Array(Set(entries.map(\.category).filter { !$0.isEmpty })).sorted()
    }

    var body: some View {
        List {
            if let summary {
                Section {
                    BestTableSummaryView(summary: summary, bestEntries: selectedBestEntries, newEntries: selectedNewEntries)
                        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                }

                BestTableSection(title: "N\(newCount)", entries: selectedNewEntries)
                BestTableSection(title: "B\(bestCount)", entries: selectedBestEntries)
            } else {
                Section {
                    ProgressView(tr("正在计算 Best 表"))
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("Best 表"))
        .scrollDismissesKeyboard(.interactively)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showSettings = true } label: {
                    Image(systemName: "slider.horizontal.3")
                }
                .tint(settingsActive ? .orange : .primary)
                .accessibilityLabel(tr("Best 表设置"))
                .accessibilityIdentifier("best-settings")
            }
            ToolbarItem(placement: .topBarTrailing) {
                if sharing {
                    ProgressView().controlSize(.small)
                } else {
                    Button(tr("分享 Best 50"), systemImage: "square.and.arrow.up") { share() }
                        .labelStyle(.iconOnly)
                        .tint(.primary)
                        .disabled(summary == nil || (selectedBestEntries.isEmpty && selectedNewEntries.isEmpty))
                        .accessibilityIdentifier("best-share")
                }
            }
        }
        .task(id: loadID) {
            reload()
        }
        .onChange(of: response?.preferences.bestCount) { _, value in
            if let value { bestCount = Int(value) }
        }
        .onChange(of: response?.preferences.newCount) { _, value in
            if let value { newCount = Int(value) }
        }
        .sheet(isPresented: $showSettings) {
            BestTableSettingsView(
                filters: $filters,
                theoretical: $theoretical,
                selectedVersion: $selectedVersion,
                bestCount: $bestCount,
                newCount: $newCount,
                serverVersion: response?.serverVersion,
                versions: response?.versions ?? [],
                difficulties: difficultyOptions,
                categories: categoryOptions,
                ranks: rankOptions,
                onCommitCapacity: commitCapacity,
                onReset: resetSettings
            )
        }
    }

    private func reload() {
        guard let bundle = catalog.bundle else {
            response = nil
            entries = []
            return
        }
        do {
            let current = try personal.bridge.bestTable(bundle: bundle, version: selectedVersion)
            response = current
            bestCount = Int(current.preferences.bestCount)
            newCount = Int(current.preferences.newCount)
            entries = try personal.bridge.allBestTableEntries(
                bundle: bundle,
                version: selectedVersion,
                theoretical: theoretical
            )
        } catch {
            personal.error = error.localizedDescription
            response = nil
            entries = []
        }
    }

    private var loadID: String {
        let version = selectedVersion ?? "auto"
        return "\(personal.revision)-\(version)-\(theoretical)"
    }

    private func commitCapacity() {
        let normalizedBest = bestCount.clamped(to: 1...99)
        let normalizedNew = newCount.clamped(to: 1...99)
        bestCount = normalizedBest
        newCount = normalizedNew
        personal.perform(catalog: catalog) {
            try personal.bridge.setBestTableCapacity(bestCount: Int32(normalizedBest), newCount: Int32(normalizedNew))
        }
    }

    private func resetSettings() {
        filters = BestTableFilterState()
        theoretical = false
        selectedVersion = nil
        bestCount = 30
        newCount = 20
        personal.perform(catalog: catalog) {
            try personal.bridge.setBestTableCapacity(
                bestCount: 30,
                newCount: 20
            )
        }
    }

    private func share() {
        guard let summary, !sharing else { return }
        sharing = true
        Task {
            do {
                let file = try await BestTableImageExporter.render(
                    catalog: catalog,
                    userName: personal.snapshot.activeProfile?.name ?? "",
                    rating: summary.displayedRating,
                    bestEntries: selectedBestEntries,
                    newEntries: selectedNewEntries,
                    bestAverage: summary.best30Average,
                    newAverage: summary.new20Average,
                    bestCount: bestCount,
                    newCount: newCount,
                    version: response?.effectiveVersion.map { CatalogVersionFormatter.shared.badge(version: $0) } ?? tr("自动"),
                    colorScheme: colorScheme
                )
                if !ActivityShare.present(items: [file]) { personal.error = tr("无法打开分享面板") }
            } catch {
                personal.error = tr("无法生成图片")
            }
            sharing = false
        }
    }
}

private extension Int {
    func clamped(to range: ClosedRange<Int>) -> Int {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
