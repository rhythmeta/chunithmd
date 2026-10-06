import SwiftUI

struct ChartPosterView: View {
    let title: String
    let subtitle: String
    let entries: [PosterEntry]
    let images: [String: UIImage]
    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            HStack(alignment: .lastTextBaseline) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("CHUNITHM").font(.system(size: 18, weight: .bold)).foregroundStyle(.orange)
                    Text(title).font(.system(size: 36, weight: .bold))
                    Text(subtitle).font(.system(size: 18)).foregroundStyle(.secondary)
                }
                Spacer()
                Text("chunithmd").font(.system(size: 18, weight: .medium)).foregroundStyle(.secondary)
            }
            VStack(spacing: 16) {
                ForEach(Array(stride(from: 0, to: entries.count, by: 5)), id: \.self) { start in
                    HStack(alignment: .top, spacing: 12) {
                        ForEach(entries[start..<min(start + 5, entries.count)]) { entry in
                            posterCell(entry)
                        }
                        Spacer(minLength: 0)
                    }
                }
            }
            Text(Date.now, format: .dateTime.year().month().day()).font(.system(size: 14)).foregroundStyle(.secondary)
        }.padding(36).frame(width: 1000).background(.white).environment(\.colorScheme, .light)
    }

    private func posterCell(_ entry: PosterEntry) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if let image = images[entry.imageName] {
                Image(uiImage: image).resizable().scaledToFill().frame(width: 176, height: 176).clipped()
            } else {
                Rectangle().fill(.quaternary).frame(width: 176, height: 176).overlay { Image(systemName: "music.note") }
            }
            Text(entry.title).font(.system(size: 15, weight: .bold)).lineLimit(1)
            Text(entry.difficulty.uppercased()).font(.system(size: 12, weight: .bold)).foregroundStyle(difficultyStyle(entry.difficulty))
            Text(entry.detail).font(.system(size: 13, weight: .medium).monospacedDigit()).lineLimit(2)
        }.frame(width: 176, alignment: .leading)
    }
}
