import SwiftUI

extension EnvironmentValues {
    @Entry var songTransitionNamespace: Namespace.ID?
}

struct SongTransitionSource: ViewModifier {
    @Environment(\.songTransitionNamespace) private var namespace
    let id: UUID

    func body(content: Content) -> some View {
        if let namespace {
            content.matchedTransitionSource(id: id, in: namespace)
        } else {
            content
        }
    }
}
