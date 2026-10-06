import SwiftUI
import Shared

struct SongDetailView: View {
    let song: CatalogSongViewData
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(RhythmetaAccountStore.self) private var account
    @Environment(SongNavigation.self) private var navigation
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var scheme
    @State private var width = 402.0
    @State private var ambient: Color?
    @State private var type = ""

    var body: some View {
        let types = Array(Set(song.sheets.map(\.type))).sorted()
        let sheets = song.sheets.filter { type.isEmpty || $0.type == type }.reversed()
        let aliases = Array(Set((catalog.bundle?.aliases[song.id] ?? []) + (account.community.approvedAliases[song.id] ?? []))).sorted()
        VStack(spacing: 0) {
            HStack {
                Button(tr("返回"), systemImage: "chevron.left") { navigation.close() }
                    .labelStyle(.iconOnly).accessibilityIdentifier("song-detail-back")
                    .frame(width: 44, height: 44).glassEffect(.regular.interactive(), in: .circle)
                Spacer()
                Text(tr("歌曲详情")).font(.system(size: 17, weight: .semibold))
                Spacer()
                Button(tr("收藏"), systemImage: personal.snapshot.favoriteSongIds.contains(song.id) ? "heart.fill" : "heart") {
                    personal.perform(catalog: catalog) { try personal.bridge.toggleFavorite(songId: song.id) }
                }.labelStyle(.iconOnly).frame(width: 44, height: 44).glassEffect(.regular.interactive(), in: .circle)
            }.tint(.primary).padding(.horizontal, 16).padding(.bottom, 8)
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: 16) {
                        JacketImage(url: catalog.jacketURL(for: song.imageName))
                            .frame(width: 220, height: 220).clipShape(.rect(cornerRadius: 28))
                            .shadow(color: .black.opacity(0.3), radius: 24, y: 12)
                            .opacity(reduceMotion || !navigation.flying ? 1 : 0)
                            .onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { navigation.arrived(at: $0) }
                        VStack(spacing: 6) {
                            Text(song.title).font(.title2.bold()).multilineTextAlignment(.center).textSelection(.enabled)
                            Text(song.artist).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                            if !aliases.isEmpty {
                                ScrollView(.horizontal) {
                                    HStack(spacing: 6) {
                                        ForEach(aliases, id: \.self) { alias in
                                            Text(alias).font(.system(size: 11, weight: .medium)).foregroundStyle(.secondary)
                                                .padding(.horizontal, 8).padding(.vertical, 4).background(.secondary.opacity(0.1), in: .capsule)
                                        }
                                    }
                                }.scrollIndicators(.hidden).padding(.top, 4)
                            }
                        }.padding(.horizontal, 32)
                    }.padding(.top, 8)
                    VStack(spacing: 20) {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                            if let bpm = song.bpm { SongMetadataPill(icon: "metronome", value: "BPM \(bpm.formatted(.number.precision(.fractionLength(0...1))))") }
                            SongMetadataPill(icon: "square.grid.2x2", value: song.category)
                            if let version = song.version { SongMetadataPill(icon: "clock", value: version) }
                            if let release = song.releaseDate { SongMetadataPill(icon: "calendar", value: release) }
                        }
                        SongAliasSection(songID: song.id)
                        SongLinksSection(song: song)
                        if types.count > 1 {
                            Picker(tr("谱面类型"), selection: $type) {
                                ForEach(types, id: \.self) { Text($0.uppercased()).tag($0) }
                            }.pickerStyle(.segmented)
                        }
                        VStack(spacing: 12) {
                            ForEach(Array(sheets)) { sheet in
                                ChartCard(song: song, sheet: sheet, initiallyExpanded: navigation.preferredSheet == sheet.id)
                            }
                        }
                    }.padding(.horizontal, 20).padding(.top, 24).padding(.bottom, 40)
                }
            }
        }
        .background((ambient ?? AppTheme.page).ignoresSafeArea())
        .onAppear { type = song.sheets.first { $0.id == navigation.preferredSheet }?.type ?? types.first ?? "" }
        .task(id: scheme) { ambient = await JacketPalette.color(for: catalog.jacketURL(for: song.imageName), dark: scheme == .dark) }
        .onGeometryChange(for: Double.self) { $0.size.width } action: { width = $0 }
        .simultaneousGesture(DragGesture(minimumDistance: 24).onChanged { value in
            if value.startLocation.x < 28 && value.translation.width > abs(value.translation.height) {
                navigation.dragBack(distance: value.translation.width, width: width)
            }
        }.onEnded { value in
            navigation.finishBack(commit: value.translation.width > width * 0.28 || value.predictedEndTranslation.width > width * 0.5)
        })
    }
}
