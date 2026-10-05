package org.rhythmeta.chunithmd.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** Keep the input responsive and update results only after typing pauses for 50 ms. */
@Composable
internal fun rememberDebouncedSearch(query: String): State<String> = produceState(query, query) {
    delay(50)
    value = query
}
