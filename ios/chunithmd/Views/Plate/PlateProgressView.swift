import SwiftUI
import Shared

struct PlateProgressView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var response: PlateProgressResponse?
    @State private var version = ""
    @State private var kind = "Spirit"
    @State private var difficulty = "master"
    @State private var remaining = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let response {
                    let scoped = response.forDifficulty(difficulty: difficulty.isEmpty ? nil : difficulty)
                    VStack(spacing: 16) {
                        PlateSummaryCard(response: scoped, difficulty: difficulty)
                        menus(response)
                    }
                    sections(scoped)
                } else {
                    ProgressView().controlSize(.large).frame(maxWidth: .infinity, minHeight: 240)
                }
            }
            .padding(.horizontal, 16).padding(.top, 16).padding(.bottom, 40)
        }
        .background(AppTheme.page)
        .navigationTitle(tr("牌子进度"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Toggle(tr("只看未完成"), isOn: $remaining)
                } label: {
                    Label(tr("只看未完成"), systemImage: remaining ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
                .tint(.primary).accessibilityIdentifier("plate-filter")
            }
        }
        .task(id: "\(personal.revision):\(catalog.allSongs.count):\(version):\(kind)") {
            guard let bundle = catalog.bundle else { return }
            do {
                response = try personal.bridge.plate(bundle: bundle, version: version.isEmpty ? nil : version, kind: kind)
                if version.isEmpty { version = response?.selectedGroup?.version ?? "" }
            } catch { personal.error = error.localizedDescription }
        }
    }

    private func menus(_ response: PlateProgressResponse) -> some View {
        HStack(spacing: 10) {
            Menu {
                Picker(tr("版本"), selection: $version) {
                    ForEach(response.groups, id: \.version) { Text($0.name).tag($0.version) }
                }
            } label: {
                PlateMenuLabel(title: tr("版本"), selection: response.selectedGroup?.name ?? "—", systemImage: "square.stack.3d.up.fill")
            }
            .accessibilityIdentifier("plate-version")
            Menu {
                Picker(tr("展示难度"), selection: $difficulty) {
                    Text(tr("全部难度")).tag("")
                    ForEach(PlateProgressCalculator.shared.difficulties, id: \.self) { Text($0.uppercased()).tag($0) }
                }
            } label: {
                PlateMenuLabel(title: tr("难度"), selection: difficulty.isEmpty ? tr("全部难度") : difficulty.uppercased(), systemImage: "dial.medium.fill")
            }
            .accessibilityIdentifier("plate-difficulty")
            Menu {
                Picker(tr("牌子类型"), selection: $kind) {
                    ForEach(PlateType.entries, id: \.name) { plate in
                        Text("\(plate.title) · \(plate.requirement)").tag(plate.name)
                    }
                }
            } label: {
                PlateMenuLabel(title: tr("牌子类型"), selection: response.plateType.title, systemImage: "sparkles.rectangle.stack.fill")
            }
            .accessibilityIdentifier("plate-kind")
        }
        .buttonStyle(.plain).tint(.primary)
    }

    @ViewBuilder
    private func sections(_ response: PlateProgressResponse) -> some View {
        let sections = response.sections(difficulty: nil, remainingOnly: false)
        let visible = sections.filter { !$0.visibleCharts(remainingOnly: remaining).isEmpty }
        let songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        if response.selectedGroup == nil {
            ContentUnavailableView(tr("暂无牌子进度"), systemImage: "music.note.list",
                                   description: Text(tr("当前服务器没有可用的 BASIC～MASTER 谱面。")))
        } else if visible.isEmpty {
            ContentUnavailableView(tr("没有符合条件的谱面"), systemImage: "music.note.list",
                                   description: Text(remaining ? tr("当前展示范围内没有未完成谱面。") : tr("试试切换展示难度。")))
        } else {
            LazyVStack(spacing: 24) {
                ForEach(visible, id: \.level) { section in
                    PlateLevelSectionView(section: section, tint: plateColor(response.plateType), remainingOnly: remaining, songs: songs)
                }
            }
        }
    }
}
