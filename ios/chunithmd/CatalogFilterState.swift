import Foundation

/// Native form values; filtering remains in the shared catalog query.
struct CatalogFilterState: Equatable, Encodable {
    var categories = Set<String>()
    var versions = Set<String>()
    var difficulties = Set<String>()
    var playableOnly = false
    var hideDeleted = false
    var favoritesOnly = false
    var minLevel = 1.0
    var maxLevel = 16.0

    var isActive: Bool { self != Self() }
}
