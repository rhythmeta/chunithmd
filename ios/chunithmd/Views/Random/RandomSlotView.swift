import SwiftUI

struct RandomSlotView: View {
    let songs: [CatalogSongViewData]
    let offset: Double
    let slotHeight: CGFloat
    let jacketSize: CGFloat
    let cornerRadius: CGFloat
    let spinning: Bool
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        ZStack {
            if let target = songs.last {
                // Keep the final cover loaded outside the moving reel, including while hidden.
                cover(target).opacity(spinning ? 0 : 1)
            } else {
                VStack {
                    Image(systemName: "questionmark.circle.fill")
                        .font(.system(size: 40)).foregroundStyle(.gray.opacity(0.3))
                    Text(tr("准备好了吗？")).font(.caption.bold()).foregroundStyle(.gray.opacity(0.4))
                }.frame(height: slotHeight).frame(maxWidth: .infinity)
            }
            if spinning {
                GeometryReader { _ in
                    VStack(spacing: 0) {
                        ForEach(songs.indices, id: \.self) { index in
                            cover(songs[index])
                        }
                    }.offset(y: offset)
                }
            }
        }
        .transaction { transaction in
            if !spinning {
                transaction.animation = nil
                transaction.disablesAnimations = true
            }
        }
        .background(.primary.opacity(0.03)).clipShape(.rect(cornerRadius: cornerRadius))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spinning ? tr("随机歌曲") : (songs.last?.title ?? tr("准备好了吗？")))
    }

    private func cover(_ song: CatalogSongViewData) -> some View {
        JacketImage(url: catalog.jacketURL(for: song.imageName))
            .frame(width: jacketSize, height: jacketSize)
            .clipShape(.rect(cornerRadius: 12))
            .shadow(color: .black.opacity(0.15), radius: 4)
            .frame(height: slotHeight).frame(maxWidth: .infinity)
            .background {
                RoundedRectangle(cornerRadius: 16).fill(.primary.opacity(0.02)).padding(2)
            }
    }
}
