import Shared
import SwiftUI

struct ConstantTableBadges: View {
    let entry: ConstantTableEntry
    var body: some View {
        VStack(alignment: .trailing, spacing: 1) {
            if let rank = entry.rank { badge(rank, color: scoreRankColor(rank)) }
            if let combo = entry.fullCombo { badge(tr(combo), color: scoreQueryComboColor(combo)) }
            if let chain = entry.fullChain { badge(tr(chain), color: scoreQueryChainColor(chain)) }
        }
    }

    private func badge(_ text: String, color: Color) -> some View {
        Text(text).font(.system(size: 8, weight: .black, design: .rounded)).foregroundStyle(.white)
            .padding(.horizontal, 3).padding(.vertical, 1)
            .background(color, in: .rect(cornerRadius: 3)).lineLimit(1).minimumScaleFactor(0.75)
    }
}
