package org.rhythmeta.chunithmd.ui.components

import androidx.compose.ui.graphics.Color
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.FullComboType
import org.rhythmeta.chunithmd.shared.FullChainType

internal fun clearStatusColor(value: String?): Color = when (ClearType.fromWire(value)) {
    ClearType.Catastrophy -> Color(0xFFAF52DE)
    ClearType.Absolute -> Color(0xFF007AFF)
    ClearType.Brave -> Color(0xFF34C759)
    ClearType.Hard -> Color(0xFFFF9500)
    ClearType.Clear -> Color(0xFF5AC8FA)
    ClearType.Failed -> Color(0xFFFF3B30)
    null -> Color(0xFF8E8E93)
}

internal fun comboStatusColor(value: String?): Color = when (FullComboType.fromWire(value)) {
    FullComboType.AllJusticeCritical,
    FullComboType.AllJustice,
    -> Color(0xFFFF9500)
    FullComboType.FullCombo -> Color(0xFF34C759)
    null -> Color(0xFF8E8E93)
}

internal fun chainStatusColor(value: String?): Color = when (FullChainType.fromWire(value)) {
    FullChainType.FullChain -> Color(0xFFB7C4D6)
    FullChainType.FullChain2 -> Color(0xFFD4A72C)
    null -> Color(0xFF8E8E93)
}
