import SwiftUI
import Shared

struct BestTableView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var sharing = false
    var body: some View {
        List {
            Section {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Rating").font(.subheadline).foregroundStyle(.secondary)
                        Text(personal.rating, format: .number.precision(.fractionLength(2)))
                            .font(.system(size: 34, weight: .black, design: .rounded)).foregroundStyle(.orange.gradient)
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("B30 · \(min(30, personal.best.filter { !$0.isNew }.count)) / 30")
                        Text("N20 · \(min(20, personal.best.filter { $0.isNew }.count)) / 20")
                    }.font(.caption).foregroundStyle(.secondary)
                }.padding(.vertical, 8)
            }
            ForEach([true, false], id: \.self) { isNew in
                Section(isNew ? "NEW 20" : "BEST 30") {
                    let entries = Array(personal.best.filter { $0.isNew == isNew }.prefix(isNew ? 20 : 30))
                    if entries.isEmpty { Text(tr("暂无成绩")).foregroundStyle(.secondary) }
                    ForEach(Array(entries.enumerated()), id: \.element.chartId) { index, entry in
                        if let song = catalog.allSongs.first(where: { $0.id == entry.songId }) {
                            SongRow(song: song, subtitle: "#\(index + 1) · \(Int(entry.score).formatted()) · R \(entry.rating.formatted(.number.precision(.fractionLength(2))))", preferredSheet: entry.type + ":" + entry.difficulty)
                        }
                    }
                }
            }
        }.navigationTitle("Best 50")
            .toolbar { Button(tr("分享成绩图"), systemImage: "square.and.arrow.up") { sharing = true }.disabled(personal.best.isEmpty) }
            .sheet(isPresented: $sharing) {
                let rows = Array(personal.best.filter { !$0.isNew }.prefix(30)) + Array(personal.best.filter { $0.isNew }.prefix(20))
                ChartPosterShareView(title: "Best 50", subtitle: "\(personal.snapshot.activeProfile?.name ?? "") · Rating \(personal.rating.formatted(.number.precision(.fractionLength(2))))", entries: rows.map {
                    PosterEntry(id: $0.chartId, title: $0.title, imageName: $0.imageName, difficulty: $0.difficulty,
                        detail: "\($0.isNew ? "NEW" : "BEST") · \(Int($0.score).formatted())\nR \($0.rating.formatted(.number.precision(.fractionLength(2))))")
                })
            }
    }
}
