import Shared
import SwiftUI

struct ScoreHistorySection: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var deleting: PersonalSnapshot.Record?
    var body: some View {
        let records = personal.history(songID: song.id, sheetID: sheet.id)
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("游玩记录")).font(.headline)
            if records.isEmpty { Text(tr("还没有成绩，记录第一次游玩吧。")).foregroundStyle(.secondary).font(.subheadline) }
            ForEach(records) { record in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(record.result.score.formatted()).font(.title3.bold().monospacedDigit())
                        Text(Date(timeIntervalSince1970: Double(record.result.achievedAt) / 1000), format: .dateTime.year().month().day().hour().minute())
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(record.result.rank).bold()
                    Button(tr("删除成绩"), systemImage: "trash", role: .destructive) { deleting = record }.labelStyle(.iconOnly).frame(width: 44, height: 44)
                }
            }
        }.frame(maxWidth: .infinity, alignment: .leading)
        .confirmationDialog(tr("删除这条成绩？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            if let deleting {
                Button(tr("删除"), role: .destructive) { personal.perform(catalog: catalog) { try personal.bridge.deleteRecord(id: deleting.id) }; self.deleting = nil }
            }
        }
    }
}
