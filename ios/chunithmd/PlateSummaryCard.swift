import SwiftUI
import Shared

struct PlateSummaryCard: View {
    let response: PlateProgressResponse
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(response.title).font(.title3.bold())
                    Text(tr("当前档案的牌子获取进度")).font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer()
                Text(Double(response.completedCount) / Double(max(1, response.totalCount)), format: .percent.precision(.fractionLength(1)))
                    .font(.headline.bold().monospacedDigit()).foregroundStyle(.blue)
                    .padding(.horizontal, 12).padding(.vertical, 8).background(.blue.opacity(0.12), in: .capsule)
            }
            ProgressView(value: Double(response.completedCount), total: Double(max(1, response.totalCount)))
            HStack(spacing: 12) {
                metric(tr("已完成"), response.completedCount)
                metric(tr("剩余"), response.remainingCount)
                metric(tr("总计"), response.totalCount)
            }
        }.padding(16).background(.ultraThinMaterial, in: .rect(cornerRadius: 20))
    }
    private func metric(_ title: String, _ count: Int32) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(count.formatted()).font(.headline.monospacedDigit())
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
}
