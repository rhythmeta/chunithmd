import SwiftUI
import Shared

struct PlateProgressView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var response: PlateProgressResponse?
    @State private var version = ""
    @State private var kind = "Spirit"
    @State private var remaining = false
    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                Picker(tr("牌子类型"), selection: $kind) { Text("Spirit").tag("Spirit"); Text("Tribute").tag("Tribute"); Text("Legend").tag("Legend") }.pickerStyle(.segmented)
                if let response {
                    Picker(tr("版本"), selection: $version) { Text(tr("默认版本")).tag(""); ForEach(response.groups, id: \.version) { Text($0.name).tag($0.version) } }
                    PlateSummaryCard(response: response)
                    Toggle(tr("只看未完成"), isOn: $remaining)
                    ForEach(response.sections(difficulty: nil, remainingOnly: remaining), id: \.level) { section in
                        Text("Lv. \(section.level)").font(.headline)
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 5), spacing: 3) {
                            ForEach(section.charts, id: \.sheetKey) { entry in
                                if let song = catalog.allSongs.first(where: { $0.id == entry.song.songId }) { SongTile(song: song, caption: entry.achievementLabel ?? entry.sheet.difficulty.uppercased(), preferredSheet: entry.sheet.type + ":" + entry.sheet.difficulty) }
                            }
                        }
                    }
                }
            }.padding(20)
        }.background(AppTheme.page).navigationTitle(tr("牌子进度"))
            .task(id: "\(personal.revision):\(version):\(kind)") {
                guard let bundle = catalog.bundle else { return }
                do { response = try personal.bridge.plate(bundle: bundle, version: version.isEmpty ? nil : version, kind: kind) } catch { personal.error = error.localizedDescription }
            }
    }
}
