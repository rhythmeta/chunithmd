import SwiftUI
import Shared

struct BestTableSection: View {
    let title: String
    let entries: [BestTableEntry]

    var body: some View {
        Section(title) {
            if entries.isEmpty {
                Text(tr("暂无可计入 {0} 的成绩", title)).foregroundStyle(.secondary)
            }
            ForEach(entries, id: \.chartId) { entry in
                BestTableRow(entry: entry)
            }
        }
    }
}
