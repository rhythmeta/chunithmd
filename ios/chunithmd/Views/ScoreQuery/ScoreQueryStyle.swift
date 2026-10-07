import Shared
import SwiftUI

func scoreQueryComboColor(_ value: String?) -> Color {
    switch ScoreQueryCalculatorKt.displayFullCombo(value: value) {
    case FullComboType.alljusticecritical.displayName: argbColor(0xFFE2B93B)
    case FullComboType.alljustice.displayName: argbColor(0xFFF0A64A)
    default: argbColor(0xFF56A6D9)
    }
}

func scoreQueryChainColor(_ value: String?) -> Color {
    ScoreQueryCalculatorKt.displayFullChain(value: value) == FullChainType.fullchain2.displayName
        ? argbColor(0xFFD5A62A) : argbColor(0xFF9EACBE)
}
