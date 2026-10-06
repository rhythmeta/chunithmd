import Shared
import SwiftUI

struct ScoreStatusBadges: View {
    let clear: String?
    let combo: String?
    let chain: String?
    let tint: AnyShapeStyle
    var showClear = true

    var body: some View {
        let comboName = FullComboType.companion.displayName(value: combo)
        let chainName = FullChainType.companion.displayName(value: chain)
        ViewThatFits(in: .horizontal) {
            badges(comboName: comboName, chainName: chainName)
            ScrollView(.horizontal) { badges(comboName: comboName, chainName: chainName) }
                .scrollIndicators(.hidden)
        }
    }

    private func badges(comboName: String?, chainName: String?) -> some View {
        HStack(spacing: 4) {
            if showClear || (comboName == nil && chainName == nil), let name = ClearType.companion.displayName(value: clear) {
                ScoreTintBadge(text: tr(name), tint: tint)
            }
            if let comboName { ScoreTintBadge(text: tr(comboName), tint: AnyShapeStyle(argbColor(0xFFFFB300))) }
            if let chainName { ScoreTintBadge(text: tr(chainName), tint: AnyShapeStyle(argbColor(0xFFB7C4D6))) }
        }
    }
}
