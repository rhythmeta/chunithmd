import SwiftUI
import Shared

struct PlateSummaryCard: View {
    let response: PlateProgressResponse
    let difficulty: String
    private var tint: Color { plateColor(response.plateType) }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(response.title).font(.title3.bold())
                    Text([response.selectedGroup?.name ?? "", difficulty.isEmpty ? tr("全部难度") : difficulty.uppercased(), response.plateType.title].joined(separator: " · "))
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer(minLength: 12)
                Text(Double(response.progress), format: .percent.precision(.fractionLength(0)))
                    .font(.headline.bold().monospacedDigit()).foregroundStyle(tint)
                    .padding(.horizontal, 12).padding(.vertical, 8).background(tint.opacity(0.12), in: .capsule)
            }
            ProgressView(value: Double(response.progress)).tint(tint)
            HStack(spacing: 12) {
                metric(tr("已完成"), response.completedCount, tint: tint)
                metric(tr("未完成"), response.remainingCount, tint: .secondary)
                metric(tr("总谱面"), response.totalCount, tint: .secondary)
            }
        }
        .padding(16).background(.ultraThinMaterial, in: .rect(cornerRadius: 20))
        .accessibilityElement(children: .contain).accessibilityIdentifier("plate-summary")
    }

    private func metric(_ title: String, _ count: Int32, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(count.formatted()).font(.headline.bold().monospacedDigit()).foregroundStyle(tint)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
}
