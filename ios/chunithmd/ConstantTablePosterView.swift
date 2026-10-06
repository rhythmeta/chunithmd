import Shared
import SwiftUI

struct ConstantTablePosterView: View {
    let title: String
    let subtitle: String
    let sections: [ConstantTableSection]
    let images: [String: UIImage]
    let includesScores: Bool
    let profileName: String?
    var sectionOffset = 0
    @Environment(\.colorScheme) private var colorScheme
    private let columns = 19
    private let jacketSize = 58.0

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(title).font(.system(size: 36, weight: .black, design: .rounded))
                        .foregroundStyle(colorScheme == .dark ? .white : argbColor(0xFF8A245C))
                    Text(subtitle).font(.system(size: 16, weight: .medium)).foregroundStyle(.secondary)
                }
                Spacer()
                if let profileName {
                    Label(profileName, systemImage: "person.crop.circle.fill").font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.purple).lineLimit(1).padding(.horizontal, 10).padding(.vertical, 6)
                        .background(.purple.opacity(0.12), in: .capsule)
                }
            }
            Divider()
            ForEach(Array(sections.enumerated()), id: \.element.constantLabel) { index, section in
                HStack(alignment: .top, spacing: 16) {
                    Text(section.constantLabel).font(.system(size: 32, weight: .black, design: .rounded))
                        .foregroundStyle(constantTableLevelColor(section.constantLabel, index: index + sectionOffset))
                        .frame(width: 72, alignment: .leading).lineLimit(1).minimumScaleFactor(0.75)
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(Array(stride(from: 0, to: section.entries.count, by: columns)), id: \.self) { start in
                            HStack(spacing: 8) {
                                ForEach(section.entries[start..<min(start + columns, section.entries.count)], id: \.sheetKey) { entry in
                                    cell(entry)
                                }
                            }
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(12).background(Color.primary.opacity((index + sectionOffset).isMultiple(of: 2) ? 0.045 : 0.025), in: .rect(cornerRadius: 18))
            }
            HStack {
                Text("chunithmd").font(.system(size: 12, weight: .medium))
                Spacer()
                Text(Date.now, format: .dateTime.year().month().day()).font(.system(size: 12))
            }.foregroundStyle(.secondary)
        }
        .padding(.horizontal, 28).padding(.vertical, 24).frame(width: 1440)
        .background(LinearGradient(colors: colorScheme == .dark
            ? [argbColor(0xFF111216), argbColor(0xFF171922), argbColor(0xFF20172B)]
            : [argbColor(0xFFFFF5FB), argbColor(0xFFFAEEFF), argbColor(0xFFF8F0FF)], startPoint: .topLeading, endPoint: .bottomTrailing))
    }

    private func cell(_ entry: ConstantTableEntry) -> some View {
        Group {
            if let image = images[entry.imageName] {
                Image(uiImage: image).resizable().scaledToFill()
            } else { Rectangle().fill(.quaternary).overlay { Image(systemName: "music.note") } }
        }
        .frame(width: jacketSize, height: jacketSize).clipShape(.rect(cornerRadius: 8))
        .overlay { RoundedRectangle(cornerRadius: 8).strokeBorder(difficultyStyle(entry.difficulty, type: entry.type), lineWidth: 2) }
        .overlay(alignment: .bottomTrailing) { if includesScores { ConstantTableBadges(entry: entry).padding(2) } }
    }
}
