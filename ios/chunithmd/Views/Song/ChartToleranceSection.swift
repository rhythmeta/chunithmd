import Shared
import SwiftUI

struct ChartToleranceSection: View {
    let total: Int
    let tint: AnyShapeStyle
    @State private var target: Int32 = 1_009_000

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(tr("容错计算器")).font(.system(size: 12, weight: .bold))
                Spacer()
            }.padding(.horizontal, 20)
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(Array(ChunithmScoreRules.shared.rankThresholds.reversed()), id: \.rank) { rank in
                        Button { target = rank.score } label: {
                            Text(rank.rank).font(.system(size: 10, weight: .bold))
                                .padding(.horizontal, 12).padding(.vertical, 8)
                                .background(target == rank.score ? tint : AnyShapeStyle(.primary.opacity(0.05)), in: .capsule)
                                .foregroundStyle(target == rank.score ? .white : .primary)
                        }.buttonStyle(.plain).accessibilityAddTraits(target == rank.score ? .isSelected : [])
                    }
                }.padding(.horizontal, 20)
            }.scrollIndicators(.hidden)
            if let result = ScoreToleranceCalculator.shared.calculate(totalNotes: KotlinInt(int: Int32(total)), targetScore: target) {
                HStack(spacing: 12) {
                    ToleranceResultView(title: "JUSTICE", value: Int(result.justice), color: .orange)
                    ToleranceResultView(title: "ATTACK", value: Int(result.attack), color: .green)
                    ToleranceResultView(title: "MISS", value: Int(result.miss), color: .gray)
                }.padding(.horizontal, 20)
            }
            Text(tr("各项独立计算，其余音符均为 JUSTICE CRITICAL，不可相加。"))
                .font(.system(size: 10)).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 20)
        }
    }
}
