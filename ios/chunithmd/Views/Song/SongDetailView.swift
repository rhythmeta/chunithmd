import SwiftUI
import Shared

struct SongDetailView: View {
    let song: CatalogSongViewData
    var preferredSheet: String? = nil
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var copyEvent: UUID?
    @State private var ambient: Color?

    var body: some View {
        let sheets = song.sheets.reversed()
        ScrollView {
            VStack(spacing: 0) {
                SongDetailHero(song: song, onCopy: copy)
                VStack(spacing: 20) {
                    SongMetadataSection(song: song, onCopy: copy)
                    SongAliasSection(songID: song.id)
                    SongLinksSection(song: song)
                    VStack(spacing: 12) {
                        ForEach(Array(sheets)) { sheet in
                            ChartCard(song: song, sheet: sheet, initiallyExpanded: preferredSheet == sheet.id)
                        }
                    }
                }.padding(.horizontal, 20).padding(.top, 24).padding(.bottom, 40)
            }
        }
        .background((ambient ?? AppTheme.page).ignoresSafeArea())
        .navigationTitle(song.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(tr("返回"), systemImage: "chevron.left") { dismiss() }
                    .labelStyle(.iconOnly).accessibilityIdentifier("song-detail-back")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("收藏"), systemImage: personal.snapshot.favoriteSongIds.contains(song.id) ? "heart.fill" : "heart") {
                    personal.perform(catalog: catalog) { try personal.bridge.toggleFavorite(songId: song.id) }
                }.labelStyle(.iconOnly)
            }
        }
        .tint(.primary)
        .overlay(alignment: .bottom) {
            if copyEvent != nil {
                Text(tr("已复制"))
                    .font(.system(size: 13, weight: .medium)).foregroundStyle(.white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(.black.opacity(0.8), in: .capsule)
                    .padding(.bottom, 24)
                    .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))
                    .allowsHitTesting(false).accessibilityIdentifier("song-copy-toast")
            }
        }
        .task(id: copyEvent) {
            guard let event = copyEvent else { return }
            do { try await Task.sleep(for: .seconds(1.5)) } catch { return }
            guard copyEvent == event else { return }
            withAnimation(reduceMotion ? nil : .default) { copyEvent = nil }
        }
        .task(id: scheme) { ambient = await JacketPalette.color(for: catalog.jacketURL(for: song.imageName), dark: scheme == .dark) }
    }

    private func copy(_ text: String) {
        UIPasteboard.general.string = text
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.85)) {
            copyEvent = UUID()
        }
    }

}
