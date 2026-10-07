import Shared
import SwiftUI

struct ChartRatingSection: View {
    let constant: Double
    let rating: Double?
    @Environment(PersonalStore.self) private var personal

    var body: some View {
        DetailDisclosure(title: tr("分数 → Rating"), trailingValue: rating.map { $0.formatted(.number.precision(.fractionLength(2))) }) {
            VStack(spacing: 0) {
                HStack(spacing: 6) {
                    Text(tr("等级")).frame(width: 36, alignment: .leading)
                    Text(tr("分数")).frame(maxWidth: .infinity, alignment: .leading)
                    Text("Rating").frame(width: 50, alignment: .trailing)
                    Text(tr("差值")).frame(width: 44, alignment: .trailing)
                }.font(.system(size: 9, weight: .bold)).foregroundStyle(.secondary)
                    .padding(.horizontal, 20).padding(.vertical, 4)
                ForEach(Array(personal.bridge.ratingTable(constant: constant).enumerated()), id: \.element.rank) { index, row in
                    HStack(spacing: 6) {
                        Text(row.rank).font(.system(size: 11, weight: .black, design: .rounded))
                            .foregroundStyle(scoreRankColor(row.rank)).frame(width: 36, alignment: .leading)
                        Text(Int(row.score).formatted()).font(.system(size: 11, design: .monospaced)).frame(maxWidth: .infinity, alignment: .leading)
                        Text(row.rating, format: .number.precision(.fractionLength(2)))
                            .font(.system(size: 12, weight: .bold, design: .monospaced)).frame(width: 50, alignment: .trailing)
                        Text(row.delta > 0 ? "↑\(row.delta.formatted(.number.precision(.fractionLength(2))))" : "")
                            .font(.system(size: 9, weight: .medium, design: .monospaced)).foregroundStyle(.secondary)
                            .frame(width: 44, alignment: .trailing)
                    }.padding(.horizontal, 20).padding(.vertical, 5)
                        .background(index.isMultiple(of: 2) ? Color.primary.opacity(0.02) : .clear)
                }
            }
        }
    }
}
