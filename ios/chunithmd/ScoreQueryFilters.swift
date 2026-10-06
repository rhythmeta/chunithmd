import Shared

/// Native form state; matching and sorting are performed by the shared query.
struct ScoreQueryFilters: Equatable {
    var difficulties: Set<String> = []
    var ranks: Set<String> = []
    var fullCombos: Set<String> = []
    var fullChains: Set<String> = []

    var shared: ScoreQueryFilterSettings {
        ScoreQueryFilterSettings(difficulties: difficulties, ranks: ranks, fullCombos: fullCombos, fullChains: fullChains)
    }
}
