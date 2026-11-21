import Foundation

/// Native editing state for the Best-table filters.
///
/// The actual filtering and slot selection stay in the shared calculator; this type only
/// keeps the selections that the settings sheet edits.
struct BestTableFilterState: Equatable {
    var difficulties: Set<String> = []
    var categories: Set<String> = []
    var ranks: Set<String> = []
    var clears: Set<String> = []
    var combos: Set<String> = []
    var chains: Set<String> = []

    var isEmpty: Bool {
        difficulties.isEmpty && categories.isEmpty && ranks.isEmpty && clears.isEmpty
            && combos.isEmpty && chains.isEmpty
    }
}
