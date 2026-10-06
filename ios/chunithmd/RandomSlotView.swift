import SwiftUI

struct RandomSlotView: View {
    let song: CatalogSongViewData?
    let spinning: Bool
    @Environment(CatalogStore.self) private var catalog
    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 10) {
                Spacer(minLength: 0)
                if let song {
                    JacketImage(url: catalog.jacketURL(for: song.imageName))
                        .frame(width: max(1, proxy.size.width - 12), height: max(1, proxy.size.width - 12))
                        .clipShape(.rect(cornerRadius: 12))
                        .id(song.id).transition(.push(from: .top))
                    if !spinning { Text(song.title).font(.system(size: 11, weight: .medium)).lineLimit(2).multilineTextAlignment(.center) }
                } else {
                    Image(systemName: "music.note").font(.system(size: 30)).foregroundStyle(.secondary.opacity(0.4))
                }
                Spacer(minLength: 0)
            }.frame(width: proxy.size.width, height: proxy.size.height)
                .background(.primary.opacity(0.03), in: .rect(cornerRadius: 14)).clipped()
                .animation(.easeOut(duration: 0.09), value: song?.id)
        }.accessibilityElement(children: .ignore).accessibilityLabel(song?.title ?? tr("等待抽曲"))
    }
}
