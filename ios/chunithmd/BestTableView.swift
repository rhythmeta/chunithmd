import SwiftUI
import Shared

struct BestTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var response: BestTableResponse?
    @State private var selectedVersion: String?
    @State private var choosingVersion = false
    @State private var sharing = false

    var body: some View {
        List {
            if let response {
                Section { BestTableSummaryView(response: response) }
                Section(tr("游戏版本")) {
                    HStack {
                        Button { choosingVersion = true } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(tr("当前版本")).font(.caption).foregroundStyle(.secondary)
                                    Text(response.effectiveVersion.map { CatalogVersionFormatter.shared.badge(version: $0) } ?? tr("未知"))
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(selectedVersion == nil ? Color.primary : .orange)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                            }.contentShape(.rect)
                        }.buttonStyle(.plain).accessibilityIdentifier("best-version")
                        if selectedVersion != nil {
                            Button(tr("重置")) { selectedVersion = nil }
                                .font(.caption).tint(.red).buttonStyle(.borderless)
                                .accessibilityIdentifier("best-version-reset")
                        }
                    }
                }
                Section(tr("容量设置")) {
                    BestTableCapacityView(preferences: response.preferences, total: response.totalCapacity) { best, new in
                        personal.perform(catalog: catalog) {
                            try personal.bridge.setBestTableCapacity(bestCount: best, newCount: new)
                        }
                    }
                }
                BestTableSection(title: "N\(response.preferences.newCount)", entries: response.newEntries)
                BestTableSection(title: "B\(response.preferences.bestCount)", entries: response.bestEntries)
            } else {
                ProgressView(tr("正在计算 Best 表"))
            }
        }
        .navigationTitle(tr("Best 表"))
        .scrollDismissesKeyboard(.interactively)
        .toolbar {
            Button(tr("分享 Best Table"), systemImage: "square.and.arrow.up") { sharing = true }
                .disabled(response == nil || (response?.bestEntries.isEmpty == true && response?.newEntries.isEmpty == true))
                .accessibilityIdentifier("best-share")
        }
        .task(id: personal.revision) { reload() }
        .onChange(of: selectedVersion) { reload() }
        .sheet(isPresented: $choosingVersion) {
            if let response {
                BestTableVersionPicker(versions: response.versions, serverVersion: response.serverVersion, selection: $selectedVersion)
            }
        }
        .sheet(isPresented: $sharing) {
            if let response {
                let rows = response.bestEntries + response.newEntries
                ChartPosterShareView(title: "Best \(response.totalCapacity)",
                    subtitle: "\(personal.snapshot.activeProfile?.name ?? "") · Rating \(response.summary.rating.formatted(.number.precision(.fractionLength(2))))", entries: rows.map {
                        PosterEntry(id: $0.chartId, title: $0.title, imageName: $0.imageName, difficulty: $0.difficulty,
                            detail: "\($0.isNew ? "NEW" : "BEST") · \(Int($0.score).formatted())\nR \($0.rating.formatted(.number.precision(.fractionLength(2))))")
                    })
            }
        }
    }

    private func reload() {
        guard let bundle = catalog.bundle else { return }
        do { response = try personal.bridge.bestTable(bundle: bundle, version: selectedVersion) }
        catch { personal.error = error.localizedDescription }
    }
}
