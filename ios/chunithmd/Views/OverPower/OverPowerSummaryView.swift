import Shared
import SwiftUI

struct OverPowerSummaryView: View {
    let points: OverPowerPoints
    let coverage: String
    let recorded: Int32
    let total: Int
    @ScaledMetric(relativeTo: .title2) private var diameter = 120.0

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 24) { ring; metrics }
                VStack(alignment: .leading, spacing: 12) { ring; metrics }
            }
            Divider()
            Text(points.nextGoal).font(.subheadline)
        }.padding(.vertical, 6)
    }

    private var ring: some View {
        ZStack {
            Circle().stroke(.secondary.opacity(0.15), lineWidth: 7)
            Circle().trim(from: 0, to: points.percent / 100)
                .stroke(points.tierStyle, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text(tr("OP 率")).font(.caption).foregroundStyle(.secondary)
                Text(points.rateText).font(.title3.bold()).monospacedDigit()
            }
        }
        .frame(width: diameter, height: diameter)
        .accessibilityElement(children: .combine)
    }

    private var metrics: some View {
        VStack(alignment: .leading, spacing: 6) {
            metric(tr("已获得 OP"), opNumber(points.earned))
            metric(tr("理论 OP"), opNumber(points.maximum))
            metric(coverage, "\(recorded) / \(total)")
        }
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(label).foregroundStyle(.secondary).font(.caption)
            Text(value).font(.subheadline.weight(.semibold)).monospacedDigit()
        }.accessibilityElement(children: .combine)
    }
}
