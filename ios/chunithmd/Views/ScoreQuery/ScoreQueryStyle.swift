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

func scoreQueryClearColor(_ value: String?) -> Color {
    switch ClearType.companion.fromWire(value: value)?.wireValue {
    case "catastrophy": argbColor(0xFFAF52DE)
    case "absolute": argbColor(0xFF007AFF)
    case "brave": argbColor(0xFF34C759)
    case "hard": argbColor(0xFFFF9500)
    case "clear": argbColor(0xFF5AC8FA)
    case "failed": argbColor(0xFFFF3B30)
    default: argbColor(0xFF8E8E93)
    }
}
