package org.rhythmeta.chunithmd.ui.overpower

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.rhythmeta.chunithmd.shared.OverPowerOrder
import org.rhythmeta.chunithmd.shared.OverPowerSort
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.ui.components.SortAction

private val orderSaver = listSaver<OverPowerOrder, Any>(
    save = { listOf(it.field.name, it.ascending) },
    restore = { saved ->
        OverPowerSort.entries.find { it.name == saved[0] }?.let { OverPowerOrder(it, saved[1] as Boolean) }
    },
)

@Composable
internal fun rememberOverPowerOrder(initial: OverPowerSort): MutableState<OverPowerOrder> =
    rememberSaveable(stateSaver = orderSaver) {
        mutableStateOf(OverPowerOrder(initial, ascending = initial == OverPowerSort.Name))
    }

@Composable
internal fun OverPowerSortMenu(state: MutableState<OverPowerOrder>, levels: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val order = state.value
    val options = remember(levels) {
        if (levels) listOf(OverPowerSort.Remaining, OverPowerSort.Rate, OverPowerSort.Level)
        else listOf(OverPowerSort.Remaining, OverPowerSort.Rate, OverPowerSort.Difficulty, OverPowerSort.Name)
    }
    SortAction(
        options = options,
        label = {
            when (it) {
                OverPowerSort.Remaining -> tr("剩余 OP")
                OverPowerSort.Difficulty -> tr("难度")
                OverPowerSort.Rate -> tr("OP 率")
                OverPowerSort.Name -> tr("曲名")
                OverPowerSort.Level -> tr("等级")
            }
        },
        expanded = expanded, sort = order.field, ascending = order.ascending,
        onToggle = { expanded = !expanded },
        onSort = { field ->
            if (order.field != field) state.value = OverPowerOrder(field, ascending = field == OverPowerSort.Name)
            expanded = false
        },
        onAscending = { state.value = order.copy(ascending = !order.ascending); expanded = false },
    )
}
