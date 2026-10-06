import Shared
import SwiftUI

struct ScoreHistorySection: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var deleting: ScoreRecord?
    @State private var expanded = false
    @State private var sort: ScoreHistorySort = .time
    @State private var page = 1
    private let pageSize = 5

    var body: some View {
        let records = personal.history(songID: song.id, sheetID: sheet.id, sort: sort)
        let tint = difficultyStyle(sheet.difficulty, type: sheet.type)
        let bestID = personal.bridge.bestHistoryRecordId(records: records)
        let totalPages = max(1, (records.count + pageSize - 1) / pageSize)
        let validPage = min(page, totalPages)
        let visible = Array(records.dropFirst((validPage - 1) * pageSize).prefix(pageSize))
        VStack(spacing: 0) {
            HStack {
                Button(action: toggle) {
                    Text(tr("历史成绩")).font(.subheadline.bold()).foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading).contentShape(.rect)
                }.accessibilityValue(expanded ? tr("已展开") : tr("已收起"))
                sortOption(tr("时间"), value: .time, tint: tint)
                sortOption(tr("分数"), value: .score, tint: tint)
                Button(action: toggle) {
                    Label(expanded ? tr("收起历史成绩") : tr("展开历史成绩"), systemImage: "chevron.right")
                        .labelStyle(.iconOnly).font(.caption.bold()).foregroundStyle(.secondary.opacity(0.4))
                        .rotationEffect(.degrees(expanded ? 90 : 0))
                }
            }.buttonStyle(.plain).padding(.horizontal, 20).padding(.bottom, expanded ? 8 : 0)
            if expanded {
                VStack(spacing: 0) {
                    ForEach(Array(visible.enumerated()), id: \.element.id) { index, record in
                        ScoreHistoryRow(record: record, isBest: record.id == bestID, alternate: index.isMultiple(of: 2), tint: tint) { deleting = record }
                    }
                    if totalPages > 1 {
                        HStack(spacing: 12) {
                            Button(tr("上一页"), systemImage: "chevron.left") { page = validPage - 1 }
                                .labelStyle(.iconOnly).padding(8).disabled(validPage <= 1)
                            Menu {
                                Picker("\(validPage) / \(totalPages)", selection: $page) {
                                    ForEach(1...totalPages, id: \.self) { number in Text("\(number)").tag(number) }
                                }
                            } label: {
                                Text("\(validPage) / \(totalPages)").font(.caption.monospaced().bold()).foregroundStyle(.primary)
                                    .padding(.horizontal, 16).padding(.vertical, 6).background(.primary.opacity(0.05), in: .capsule)
                            }
                            Button(tr("下一页"), systemImage: "chevron.right") { page = validPage + 1 }
                                .labelStyle(.iconOnly).padding(8).disabled(validPage >= totalPages)
                        }.font(.subheadline.bold()).foregroundStyle(tint).buttonStyle(.plain).padding(.vertical, 12)
                    }
                }.transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .onChange(of: totalPages) { page = min(page, totalPages) }
        .confirmationDialog(tr("确定删除这条历史成绩吗？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            if let deleting {
                Button(tr("删除"), role: .destructive) { personal.perform(catalog: catalog) { try personal.bridge.deleteRecord(id: deleting.id) }; self.deleting = nil }
            }
        }
    }

    private func toggle() {
        withAnimation(reduceMotion ? nil : .spring(duration: 0.35, bounce: 0.2)) { expanded.toggle() }
    }

    private func sortOption(_ title: String, value: ScoreHistorySort, tint: AnyShapeStyle) -> some View {
        Button { sort = value; page = 1 } label: {
            Text(title).font(.caption).foregroundStyle(sort == value ? tint : AnyShapeStyle(.secondary))
                .padding(.horizontal, 7).padding(.vertical, 5)
                .background(sort == value ? AnyShapeStyle(tint.opacity(0.12)) : AnyShapeStyle(.clear), in: .rect(cornerRadius: 8))
        }.accessibilityAddTraits(sort == value ? .isSelected : [])
    }
}
