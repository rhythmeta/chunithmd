import Shared
import SwiftUI

struct PlateChartTile: View {
    let entry: PlateChartEntry
    let tint: Color
    let song: CatalogSongViewData?
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor
    @State private var identity = UUID()

    var body: some View {
        Button {
            if let song { navigation.open(song, sheetID: entry.sheet.type + ":" + entry.sheet.difficulty, sourceID: identity) }
        } label: {
            JacketImage(url: catalog.jacketURL(for: entry.song.imageName))
                .aspectRatio(1, contentMode: .fit)
                .saturation(entry.achieved ? 1 : 0.08)
                .brightness(entry.achieved ? -0.08 : 0)
                .overlay(alignment: .bottom) {
                    if entry.achieved {
                        LinearGradient(colors: [.clear, tint.opacity(0.75)], startPoint: .top, endPoint: .bottom).frame(height: 22)
                    }
                }
                .clipShape(.rect(cornerRadius: 12))
                .overlay {
                    if !entry.achieved && differentiateWithoutColor {
                        Image(systemName: "circle.dashed").font(.title3).foregroundStyle(.white.opacity(0.85)).shadow(radius: 4)
                    }
                }
                .overlay(alignment: .bottomTrailing) {
                    if let label = entry.achievementLabel {
                        Text(label).font(.system(size: 8, weight: .bold, design: .rounded)).foregroundStyle(.white)
                            .padding(.horizontal, 4).padding(.vertical, 2.5)
                            .background((label == "S" || label == "S+" || label == "SS" || label == "SS+" ? argbColor(0xFFFF8F00) : argbColor(0xFFE2B93B)).opacity(0.95), in: .capsule)
                            .shadow(color: .black.opacity(0.25), radius: 2, y: 1).padding(4)
                    }
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 12).strokeBorder(entry.achieved ? tint.opacity(0.7) : Color.primary.opacity(0.08), lineWidth: entry.achieved ? 2 : 1)
                }
        }
        .buttonStyle(.plain).disabled(song == nil)
        .modifier(SongTransitionSource(id: identity))
        .accessibilityIdentifier("plate-tile-" + entry.sheetKey)
        .accessibilityLabel([song?.title ?? entry.song.title, entry.sheet.difficulty.uppercased()].joined(separator: ", "))
        .accessibilityValue([entry.achieved ? tr("已完成") : tr("未完成"), entry.achievementLabel ?? ""].joined(separator: " "))
    }
}
