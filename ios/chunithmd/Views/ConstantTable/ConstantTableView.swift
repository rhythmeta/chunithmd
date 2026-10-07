import Shared
import SwiftUI

struct ConstantTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var sharing = false
    @State private var showingFilters = false
    @State private var response: ConstantTableResponse?
    @State private var filters = CatalogFilterState()
    @State private var level: Int32 = 14
    @State private var includesScores = false
    @State private var levels: [Int32] = []
    @State private var sections: [ConstantTableSection] = []
    @State private var songs: [String: CatalogSongViewData] = [:]

    private var entries: [ConstantTableEntry] { sections.flatMap(\.entries) }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                if response == nil {
                    CatalogLoadingView()
                } else if levels.isEmpty {
                    ContentUnavailableView(tr("没有可用的定数数据"), systemImage: "music.note.list",
                        description: Text(tr("请先更新歌曲目录，或调整筛选条件。")))
                } else {
                    settingsCard
                    HStack {
                        Label(tr("普通谱面定数表"), systemImage: "music.note")
                        Spacer(minLength: 8)
                        Text(tr("{0} 张谱面 · {1} 个定数{2}", entries.count, sections.count, filters.isActive ? tr(" · 已筛选") : ""))
                            .foregroundStyle(.secondary).monospacedDigit()
                    }
                    .font(.footnote).padding(16)
                    .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 16))
                    .accessibilityIdentifier("constant-table-summary")
                    Button { sharing = true } label: {
                        Label(tr("导出定数表图片"), systemImage: "photo.on.rectangle.angled")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent).controlSize(.large)
                    .disabled(sections.isEmpty).accessibilityIdentifier("constant-table-export")
                    LazyVStack(alignment: .leading, spacing: 16) {
                        ForEach(Array(sections.enumerated()), id: \.element.constantLabel) { index, section in
                            ConstantTableSectionView(section: section, index: index, includesScores: includesScores, songs: songs)
                        }
                    }.padding(.top, 4).padding(.bottom, 4)
                }
            }.padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(tr("定数表"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("筛选"), systemImage: filters.isActive ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle") { showingFilters = true }
                    .labelStyle(.iconOnly).tint(.primary).accessibilityIdentifier("constant-table-filter")
            }
        }
        .sheet(isPresented: $showingFilters) { ConstantTableFilterView(settings: $filters) }
        .sheet(isPresented: $sharing) {
            ChartPosterShareView(title: tr("Lv. {0} 定数表", ConstantTableCalculatorKt.constantTableBaseLevelLabel(baseLevel: level)),
                subtitle: tr("{0} 个定数 · {1} 张谱面", sections.count, entries.count),
                entries: entries.map { PosterEntry(id: $0.sheetKey, title: $0.title, imageName: $0.imageName, difficulty: $0.difficulty, detail: "") },
                constantSections: sections, includesScores: includesScores,
                profileName: includesScores ? personal.snapshot.activeProfile?.name : nil)
        }
        .task(id: personal.revision) { reload() }
        .onChange(of: filters) { rebuildSections() }
        .onChange(of: level) { rebuildSections() }
    }

    private var settingsCard: some View {
        VStack(spacing: 0) {
            HStack {
                Label(tr("定数档位"), systemImage: "chart.bar.fill")
                Spacer()
                Menu {
                    Picker(tr("定数档位"), selection: $level) {
                        ForEach(levels, id: \.self) { value in
                            Text(ConstantTableCalculatorKt.constantTableBaseLevelLabel(baseLevel: value)).tag(value)
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Text(ConstantTableCalculatorKt.constantTableBaseLevelLabel(baseLevel: level))
                        Image(systemName: "chevron.up.chevron.down").font(.caption.weight(.semibold))
                    }
                    .frame(minWidth: 51, minHeight: 44, alignment: .trailing)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain).foregroundStyle(.secondary)
                .accessibilityLabel(tr("定数档位"))
                .accessibilityValue(ConstantTableCalculatorKt.constantTableBaseLevelLabel(baseLevel: level))
                .accessibilityIdentifier("constant-table-level")
            }
            .frame(minHeight: 44).padding(.vertical, 8).padding(. horizontal, 16)
            Divider().padding(.horizontal, 16)
            Toggle(isOn: $includesScores) {
                Label(tr("显示成绩徽标"), systemImage: includesScores ? "person.text.rectangle.fill" : "person.text.rectangle")
            }
            .frame(minHeight: 44).padding(.vertical, 8).padding(.horizontal, 16)
            .accessibilityIdentifier("constant-table-scores")
        }
        .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 16))
    }

    private func reload() {
        guard let bundle = catalog.bundle else { return }
        do {
            response = try personal.bridge.constants(bundle: bundle)
            songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
            rebuildSections()
        } catch { personal.error = error.localizedDescription }
    }

    private func rebuildSections() {
        let settings = CatalogFilters(categories: filters.categories, versions: filters.versions, difficulties: [], types: [],
            minLevel: 1, maxLevel: 16, playableOnly: false, hideDeleted: false, favoritesOnly: filters.favoritesOnly)
        let filtered = ConstantTableResponse(entries: ConstantTableCalculatorKt.filterConstantTableEntries(entries: response?.entries ?? [], filters: settings))
        levels = filtered.availableBaseLevels.map(\.int32Value)
        if !levels.contains(level), let first = levels.first { level = first }
        sections = filtered.sections(baseLevel: level)
    }
}
