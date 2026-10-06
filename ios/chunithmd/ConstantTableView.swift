import SwiftUI
import Shared

struct ConstantTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var sharing = false
    @State private var response: ConstantTableResponse?
    @State private var level: Int32 = 14
    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                if let response {
                    Picker(tr("等级"), selection: $level) {
                        ForEach(response.availableBaseLevels, id: \.self) { value in Text("Lv. \(value.intValue)").tag(value.int32Value) }
                    }.pickerStyle(.menu).padding(16)
                        .frame(maxWidth: .infinity).background(AppTheme.surface, in: .rect(cornerRadius: 16)).padding(.horizontal, 16)
                    ForEach(response.sections(baseLevel: level), id: \.constantLabel) { section in
                        Text(section.constantLabel).font(.title2.bold()).padding(.horizontal)
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 5), spacing: 3) {
                            ForEach(section.entries, id: \.sheetKey) { entry in
                                if let song = catalog.allSongs.first(where: { $0.id == entry.songId }) { SongTile(song: song, caption: entry.rank ?? entry.difficulty.uppercased(), preferredSheet: entry.type + ":" + entry.difficulty) }
                            }
                        }
                    }
                } else { CatalogLoadingView() }
            }.padding(.vertical)
        }.background(AppTheme.page).navigationTitle(tr("定数表"))
            .toolbar { Button(tr("分享定数表"), systemImage: "square.and.arrow.up") { sharing = true }.disabled(response == nil) }
            .sheet(isPresented: $sharing) {
                let entries = response?.sections(baseLevel: level).flatMap { $0.entries } ?? []
                ChartPosterShareView(title: tr("Lv. {0} 定数表", level), subtitle: tr("{0} 个谱面 · {1}", entries.count, (personal.snapshot.activeProfile?.server ?? "jp").uppercased()), entries: entries.map {
                    PosterEntry(id: $0.sheetKey, title: $0.title, imageName: $0.imageName, difficulty: $0.difficulty,
                        detail: "\($0.constant.formatted(.number.precision(.fractionLength(1)))) · \($0.rank ?? "—")")
                })
            }
            .task(id: personal.revision) {
                guard let bundle = catalog.bundle else { return }
                do { response = try personal.bridge.constants(bundle: bundle) } catch { personal.error = error.localizedDescription }
            }
    }
}
