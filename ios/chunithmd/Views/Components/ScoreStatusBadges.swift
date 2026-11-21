import Shared
import SwiftUI

struct ScoreStatusBadges: View {
    let clear: String?
    let combo: String?
    let chain: String?
    let tint: AnyShapeStyle
    var showClear = true
    var font: Font = .caption.bold()

    var body: some View {
        let comboName = comboLabel(combo)
        let chainName = chainLabel(chain)
        ViewThatFits(in: .horizontal) {
            badges(comboName: comboName, chainName: chainName)
            ScrollView(.horizontal) { badges(comboName: comboName, chainName: chainName) }
                .scrollIndicators(.hidden)
        }
    }

    private func badges(comboName: String?, chainName: String?) -> some View {
        HStack(spacing: 4) {
            if showClear || (comboName == nil && chainName == nil), let name = ClearType.companion.displayName(value: clear) {
                ScoreTintBadge(text: tr(name), tint: tint, font: font)
            }
            if let comboName { ScoreTintBadge(text: tr(comboName), tint: AnyShapeStyle(argbColor(0xFFFFB300)), font: font) }
            if let chainName { ScoreTintBadge(text: tr(chainName), tint: AnyShapeStyle(argbColor(0xFFB7C4D6)), font: font) }
        }
    }

    private func comboLabel(_ value: String?) -> String? {
        switch value?.lowercased() {
        case "alljusticecritical": "AJC"
        case "alljustice": "AJ"
        case "fullcombo": "FC"
        default: FullComboType.companion.displayName(value: value)
        }
    }

    private func chainLabel(_ value: String?) -> String? {
        switch value?.lowercased() {
        case "fullchain": tr("铂 FC")
        case "fullchain2": tr("金 FC")
        default: FullChainType.companion.displayName(value: value)
        }
    }
}
