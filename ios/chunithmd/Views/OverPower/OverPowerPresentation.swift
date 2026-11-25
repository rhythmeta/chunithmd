import Shared
import SwiftUI

func opNumber(_ value: Double) -> String {
    value.formatted(.number.precision(.fractionLength(2...3)))
}

extension OverPowerPoints {
    var rateText: String {
        (percent / 100).formatted(.percent.precision(.fractionLength(2)))
    }

    var tierStyle: AnyShapeStyle {
        switch tier {
        case .silver: AnyShapeStyle(Color.gray)
        case .gold: AnyShapeStyle(Color(red: 0.72, green: 0.53, blue: 0.15))
        case .platinum: AnyShapeStyle(Color(red: 0.45, green: 0.61, blue: 0.69))
        case .rainbow: AnyShapeStyle(LinearGradient(colors: [.pink, .orange, .mint, .indigo], startPoint: .leading, endPoint: .trailing))
        default: AnyShapeStyle(.primary)
        }
    }

    var nextGoal: String {
        guard pointsToNext > 0 else { return tr("已达理论值") }
        let name: String
        switch nextTier {
        case .silver: name = tr("银色")
        case .gold: name = tr("金色")
        case .platinum: name = tr("铂色")
        case .rainbow: name = tr("虹色")
        default: return tr("距 100.00% 还差 {0} OP", opNumber(pointsToNext))
        }
        let target = (Double(nextPercent) / 100).formatted(.percent.precision(.fractionLength(2)))
        return tr("距{0} {1} 还差 {2} OP", name, target, opNumber(pointsToNext))
    }
}
