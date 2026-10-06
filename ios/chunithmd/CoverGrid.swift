import SwiftUI

struct CoverGrid: View {
    let songs: [CatalogSongViewData]
    var captions: [String] = []
    var sheetIDs: [String] = []
    @AppStorage private var savedColumns: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var zoom = 1.0
    @State private var initialZoom: Double?
    @State private var offset = 0.0
    @State private var viewport = 800.0
    @State private var width = 400.0
    @State private var anchorIndex = 0
    @State private var anchorY = 0.0
    @State private var scroll = ScrollPosition()

    init(songs: [CatalogSongViewData], captions: [String] = [], sheetIDs: [String] = [], preferenceKey: String = "catalog.gridColumns") {
        self.songs = songs; self.captions = captions; self.sheetIDs = sheetIDs
        _savedColumns = AppStorage(wrappedValue: 5, preferenceKey)
    }

    var body: some View {
        let geometry = CatalogGridGeometry(width: width, count: songs.count, zoom: zoom)
        ScrollView {
            ZStack(alignment: .topLeading) {
                Color.clear.frame(height: geometry.height)
                ForEach(geometry.visible(top: offset, height: viewport), id: \.self) { index in
                    let rect = geometry.frame(index)
                    SongTile(song: songs[index], caption: captions.indices.contains(index) ? captions[index] : nil, preferredSheet: sheetIDs.indices.contains(index) ? sheetIDs[index] : nil)
                        .frame(width: rect.width, height: rect.height)
                        .offset(x: rect.minX, y: rect.minY)
                }
            }
            .frame(height: geometry.height, alignment: .topLeading)
        }
        .scrollPosition($scroll)
        .scrollDisabled(initialZoom != nil)
        .onScrollGeometryChange(for: Double.self) { $0.contentOffset.y + $0.contentInsets.top } action: { _, value in offset = max(0, value) }
        .onGeometryChange(for: CGSize.self) { $0.size } action: { width = $0.width; viewport = $0.height }
        .simultaneousGesture(MagnifyGesture().onChanged { value in
            if initialZoom == nil {
                initialZoom = zoom
                let focusY = value.startAnchor.y * viewport
                anchorIndex = min(max(0, songs.count - 1), Int((offset + focusY) / geometry.step) * geometry.columns + geometry.columns / 2)
                anchorY = geometry.frame(anchorIndex).midY - offset
            }
            let base = CatalogGridGeometry(width: width, count: songs.count, zoom: initialZoom ?? zoom)
            let desiredSize = base.size * value.magnification
            zoom = min(2, max(1, 1 + (desiredSize - base.sizeFor(5)) / (base.sizeFor(3) - base.sizeFor(5))))
            preserveAnchor()
        }.onEnded { _ in
            let target = zoom < 1.5 ? 1.0 : 2.0
            withAnimation(reduceMotion ? nil : .spring(duration: 0.3, bounce: 0)) {
                zoom = target
                preserveAnchor()
            }
            savedColumns = target == 1 ? 5 : 3
            initialZoom = nil
        })
        .onAppear { zoom = savedColumns == 3 ? 2 : 1 }
        .onChange(of: savedColumns) { if initialZoom == nil { zoom = savedColumns == 3 ? 2 : 1 } }
        .accessibilityAction(named: tr("放大网格")) { savedColumns = 3 }
        .accessibilityAction(named: tr("缩小网格")) { savedColumns = 5 }
        .clipped()
    }

    private func preserveAnchor() {
        let geometry = CatalogGridGeometry(width: width, count: songs.count, zoom: zoom)
        let target = max(0, min(max(0, geometry.height - viewport), geometry.frame(anchorIndex).midY - anchorY))
        scroll.scrollTo(y: target)
    }
}
