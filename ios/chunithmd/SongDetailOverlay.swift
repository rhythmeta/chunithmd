import SwiftUI

struct SongDetailOverlay: View {
    @Environment(SongNavigation.self) private var navigation
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        GeometryReader { proxy in
            if let song = navigation.song {
                let p = navigation.progress
                let origin = proxy.frame(in: .global).origin
                let source = navigation.source
                let target = navigation.target
                let rect = CGRect(x: source.minX + (target.minX - source.minX) * p,
                                  y: source.minY + (target.minY - source.minY) * p,
                                  width: source.width + (target.width - source.width) * p,
                                  height: source.height + (target.height - source.height) * p)
                ZStack(alignment: .topLeading) {
                    Rectangle().fill(.background).ignoresSafeArea().opacity(p)
                    SongDetailView(song: song)
                        .opacity(p)
                        .allowsHitTesting(!navigation.preparing && !navigation.returning)
                    if !reduceMotion && navigation.flying {
                        JacketImage(url: catalog.jacketURL(for: song.imageName))
                            .frame(width: rect.width, height: rect.height)
                            .clipShape(.rect(cornerRadius: navigation.sourceRadius + (28 - navigation.sourceRadius) * p))
                            .offset(x: rect.minX - origin.x, y: rect.minY - origin.y)
                            .allowsHitTesting(false)
                    }
                }
                .accessibilityAction(.escape) { navigation.close() }
            }
        }
        .onChange(of: reduceMotion, initial: true) { navigation.reduceMotion = reduceMotion }
    }
}
