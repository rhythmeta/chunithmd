import SwiftUI
import Shared

struct BestTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.colorScheme) private var colorScheme
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
            // The image is rendered first and handed straight to the system share sheet.
            if sharing {
                ProgressView().controlSize(.small).tint(.primary)
            } else {
                Button(tr("分享 Best Table"), systemImage: "square.and.arrow.up") { share() }
                    // Follows the theme like every other toolbar button: white on dark, black on light.
                    .tint(.primary)
                    .disabled(response == nil || (response?.bestEntries.isEmpty == true && response?.newEntries.isEmpty == true))
                    .accessibilityIdentifier("best-share")
            }
        }
        .task(id: personal.revision) { reload() }
        .onChange(of: selectedVersion) { reload() }
        .sheet(isPresented: $choosingVersion) {
            if let response {
                BestTableVersionPicker(versions: response.versions, serverVersion: response.serverVersion, selection: $selectedVersion)
            }
        }
    }

    /// Renders the Best table and opens the system share sheet directly, without an in-app preview.
    private func share() {
        guard let response, !sharing else { return }
        sharing = true
        Task {
            do {
                let file = try await BestTableImageExporter.render(
                    catalog: catalog,
                    userName: personal.snapshot.activeProfile?.name ?? "",
                    rating: response.summary.rating,
                    bestEntries: response.bestEntries,
                    newEntries: response.newEntries,
                    bestAverage: response.summary.best30Average,
                    newAverage: response.summary.new20Average,
                    bestCount: Int(response.preferences.bestCount),
                    newCount: Int(response.preferences.newCount),
                    version: response.effectiveVersion.map { CatalogVersionFormatter.shared.badge(version: $0) } ?? tr("自动"),
                    colorScheme: colorScheme
                )
                if !ActivityShare.present(items: [file]) { personal.error = tr("无法打开分享面板") }
            } catch {
                personal.error = tr("无法生成图片")
            }
            sharing = false
        }
    }

    private func reload() {
        guard let bundle = catalog.bundle else { return }
        do { response = try personal.bridge.bestTable(bundle: bundle, version: selectedVersion) }
        catch { personal.error = error.localizedDescription }
    }
}
