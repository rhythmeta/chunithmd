import CoreText
import Foundation
import SwiftUI

// MARK: - Poster model

/// A CLEAR / FULL COMBO / FULL CHAIN pill drawn inside a song card.
struct BestTablePosterBadge: Identifiable {
    let id: String
    let text: String
    let tint: Color
}

/// One song card of an exported Best table.
struct BestTablePosterEntry: Identifiable {
    let id: String
    let title: String
    let difficulty: String
    let score: Int
    let rank: String
    let rating: Double
    /// Display level, already formatted for the card ("13.7").
    let level: String
    let badges: [BestTablePosterBadge]
    let jacket: Image?
}

/// Header pill summarizing a section ("N20 15.82").
struct BestTablePosterSummary: Identifiable {
    let id: String
    let title: String
    let average: Double
    let accent: Color
}

/// A titled grid of entries ("N20" / "B30") with its average Rating.
struct BestTablePosterSection: Identifiable {
    let id: String
    let title: String
    let average: Double
    let accent: Color
    let entries: [BestTablePosterEntry]
}

// MARK: - Layout

/// Logical geometry of the exported image.
///
/// Every value mirrors `BestTableImageExporter` on the Android client so both platforms
/// export the same Best-table artwork. The view is rendered at 2x, matching the Android
/// `OUTPUT_SCALE`.
enum BestTablePosterLayout {
    static let canvasWidth: CGFloat = 1180
    static let outputScale: CGFloat = 2
    static let columns = 5
    static let cardWidth: CGFloat = 220
    static let cardHeight: CGFloat = 74
    static let cardSpacing: CGFloat = 8
    static let sectionPadding: CGFloat = 24
    static let jacketSize: CGFloat = 62
    static let headerHeight: CGFloat = 184
    static let sectionHeaderHeight: CGFloat = 50
    static let sectionBottomPadding: CGFloat = 16
    static let footerHeight: CGFloat = 70
    static let newAccent = argbColor(0xFFFF6B6B)
    static let bestAccent = argbColor(0xFF4D80FF)

    static func rows(_ entryCount: Int) -> Int { (entryCount + columns - 1) / columns }

    static func sectionHeight(_ entryCount: Int) -> CGFloat {
        sectionHeaderHeight + CGFloat(rows(entryCount)) * (cardHeight + cardSpacing) + sectionBottomPadding
    }
}

/// Light and dark surfaces of the exported image.
struct BestTablePosterPalette {
    let dark: Bool

    var backgroundHex: Int64 { dark ? 0xFF0F0F13 : 0xFFFFFFFF }
    var background: Color { argbColor(backgroundHex) }
    var primary: Color { argbColor(dark ? 0xFFFFFFFF : 0xFF000000) }
    var secondary: Color { argbColor(dark ? 0xFF9E9EA0 : 0xFF666666) }
    var subtle: Color { argbColor(dark ? 0xFF57575A : 0xFFB3B3B3) }
    var emptyCard: Color { argbColor(dark ? 0xFF1B1B1F : 0xFFF2F2F2) }
}

// MARK: - Colors

/// Status colors of the exported image, matching the Android exporter palette.
enum BestTablePosterColors {
    static func difficultyHex(_ difficulty: String) -> Int64 {
        let value = difficulty.lowercased()
        if value.contains("basic") { return 0xFF36BF63 }
        if value.contains("advanced") { return 0xFFFCA13B }
        if value.contains("expert") { return 0xFFF7536A }
        if value.contains("remaster") { return 0xFFE3BDFC }
        if value.contains("master") { return 0xFFA34EE4 }
        return 0xFFFF2D55
    }

    static func difficulty(_ difficulty: String) -> Color { argbColor(difficultyHex(difficulty)) }

    static func rank(_ rank: String) -> Color {
        switch rank.uppercased() {
        case "SSS+", "SSS": argbColor(0xFFFFD900)
        case "SS+", "SS": argbColor(0xFFFFBF00)
        case "S+", "S": argbColor(0xFFFF9900)
        case "AAA": argbColor(0xFFCC99FF)
        case "AA": argbColor(0xFF99CCFF)
        case "A": argbColor(0xFF80E680)
        default: argbColor(0xFFCCCCCC)
        }
    }

    static func clear(_ value: String?) -> Color {
        switch normalized(value) {
        case "catastrophy": argbColor(0xFFAF52DE)
        case "absolute": argbColor(0xFF007AFF)
        case "brave": argbColor(0xFF34C759)
        case "hard": argbColor(0xFFFF9500)
        case "clear": argbColor(0xFF5AC8FA)
        case "failed": argbColor(0xFFFF3B30)
        default: argbColor(0xFFCCCCCC)
        }
    }

    static func combo(_ value: String?) -> Color {
        switch normalized(value) {
        case "alljusticecritical", "alljustice": argbColor(0xFFFF9900)
        case "fullcombo": argbColor(0xFF33BF33)
        default: argbColor(0xFFCCCCCC)
        }
    }

    static func chain(_ value: String?) -> Color {
        switch normalized(value) {
        case "fullchain": argbColor(0xFFB7C4D6)
        case "fullchain2": argbColor(0xFFD4A72C)
        default: argbColor(0xFFCCCCCC)
        }
    }

    private static func normalized(_ value: String?) -> String {
        value?.trimmingCharacters(in: .whitespaces).lowercased() ?? ""
    }
}

// MARK: - Poster

/// The exported Best-table image, drawn to match the Android `BestTableImageExporter` canvas.
///
/// The view has no dependency on the shared framework or UIKit so it can be laid out and
/// rendered on any Apple platform through `ImageRenderer`.
struct BestTablePosterView: View {
    static let canvasWidth = BestTablePosterLayout.canvasWidth

    let userName: String
    let ratingLabel: String
    let rating: Double
    let summaries: [BestTablePosterSummary]
    let sections: [BestTablePosterSection]
    let versionText: String
    let appIcon: Image?
    let colorScheme: ColorScheme

    private var palette: BestTablePosterPalette { BestTablePosterPalette(dark: colorScheme == .dark) }

    var body: some View {
        VStack(spacing: 0) {
            header
            ForEach(sections) { section in
                sectionView(section)
            }
            footer
        }
        .frame(width: Self.canvasWidth)
        .background(palette.background)
        .environment(\.colorScheme, colorScheme)
    }

    // MARK: Header

    private var header: some View {
        let name = userName.trimmingCharacters(in: .whitespacesAndNewlines)
        return ZStack(alignment: .topLeading) {
            if !name.isEmpty {
                baselineText(name, size: 24, weight: .bold, color: palette.primary, baseline: 64)
                    .offset(x: BestTablePosterLayout.sectionPadding)
            }
            baselineText(ratingLabel, size: 14, weight: .regular, color: palette.secondary, baseline: name.isEmpty ? 72 : 95)
                .offset(x: BestTablePosterLayout.sectionPadding)
            Text(posterRating(rating))
                .font(.system(size: 56, weight: .bold))
                .foregroundStyle(LinearGradient(colors: playerRatingColors(rating), startPoint: .leading, endPoint: .trailing))
                .frame(width: Self.canvasWidth - BestTablePosterLayout.sectionPadding, alignment: .trailing)
                .offset(y: baselineOffset(84, size: 56, weight: .bold))
            HStack(spacing: 12) {
                ForEach(summaries) { summary in
                    summaryPill(summary)
                }
            }
            .offset(x: BestTablePosterLayout.sectionPadding, y: 126)
        }
        .frame(width: Self.canvasWidth, height: BestTablePosterLayout.headerHeight, alignment: .topLeading)
    }

    private func summaryPill(_ summary: BestTablePosterSummary) -> some View {
        HStack(spacing: 6) {
            Circle().fill(summary.accent).frame(width: 8, height: 8)
            Text(summary.title).font(.system(size: 13)).foregroundStyle(palette.secondary)
            Text(posterRating(summary.average)).font(.system(size: 13, weight: .bold)).foregroundStyle(summary.accent)
        }
        .padding(.horizontal, 12)
        .frame(height: 32)
        .background(summary.accent.opacity(0.12), in: .rect(cornerRadius: 16, style: .circular))
    }

    // MARK: Sections

    private func sectionView(_ section: BestTablePosterSection) -> some View {
        let rows = stride(from: 0, to: section.entries.count, by: BestTablePosterLayout.columns).map { start in
            Array(section.entries[start..<min(start + BestTablePosterLayout.columns, section.entries.count)])
        }
        return ZStack(alignment: .topLeading) {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(section.title).font(.system(size: 20, weight: .bold)).foregroundStyle(palette.primary)
                Text(posterRating(section.average)).font(.system(size: 14, weight: .bold)).foregroundStyle(section.accent)
            }
            .offset(x: BestTablePosterLayout.sectionPadding, y: baselineOffset(37, size: 20, weight: .bold))
            ForEach(Array(rows.enumerated()), id: \.offset) { rowIndex, row in
                ForEach(Array(row.enumerated()), id: \.element.id) { column, entry in
                    card(entry)
                        .offset(
                            x: BestTablePosterLayout.sectionPadding + CGFloat(column) * (BestTablePosterLayout.cardWidth + BestTablePosterLayout.cardSpacing),
                            y: BestTablePosterLayout.sectionHeaderHeight + CGFloat(rowIndex) * (BestTablePosterLayout.cardHeight + BestTablePosterLayout.cardSpacing)
                        )
                }
            }
        }
        .frame(width: Self.canvasWidth, height: BestTablePosterLayout.sectionHeight(section.entries.count), alignment: .topLeading)
    }

    // MARK: Card

    private func card(_ entry: BestTablePosterEntry) -> some View {
        let difficulty = BestTablePosterColors.difficulty(entry.difficulty)
        let shape = RoundedRectangle(cornerRadius: 6, style: .circular)
        return ZStack(alignment: .topLeading) {
            shape.fill(composite(BestTablePosterColors.difficultyHex(entry.difficulty), over: palette.backgroundHex))
            shape.strokeBorder(difficulty.opacity(0.34), lineWidth: 1)
            jacket(entry.jacket)
                .offset(x: 6, y: 6)
            baselineText(entry.title, size: 11, weight: .bold, color: palette.primary, baseline: 19)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(width: BestTablePosterLayout.cardWidth - 82, alignment: .leading)
                .offset(x: 76)
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(entry.rank).font(.system(size: 12, weight: .bold)).foregroundStyle(BestTablePosterColors.rank(entry.rank))
                Text(posterScore(entry.score)).font(.system(size: 9)).foregroundStyle(palette.secondary)
            }
            .offset(x: 76, y: baselineOffset(37, size: 12, weight: .bold))
            baselineText("\(entry.level) → \(posterRating(entry.rating))", size: 9, weight: .bold, color: difficulty, baseline: 53)
                .offset(x: 76)
            HStack(spacing: 3) {
                ForEach(entry.badges) { badge in
                    Text(badge.text)
                        .font(.system(size: 7, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 3)
                        .frame(height: 12)
                        .background(badge.tint, in: .rect(cornerRadius: 2, style: .circular))
                }
            }
            .offset(x: 76, y: 55)
        }
        .frame(width: BestTablePosterLayout.cardWidth, height: BestTablePosterLayout.cardHeight, alignment: .topLeading)
    }

    private func jacket(_ cover: Image?) -> some View {
        ZStack(alignment: .topLeading) {
            if let cover {
                cover.resizable().scaledToFill()
            } else {
                Rectangle().fill(palette.emptyCard)
                baselineText("♪", size: 22, weight: .regular, color: palette.subtle, baseline: 40)
                    .offset(x: 24)
            }
        }
        .frame(width: BestTablePosterLayout.jacketSize, height: BestTablePosterLayout.jacketSize, alignment: .topLeading)
        .clipShape(.rect(cornerRadius: 4, style: .circular))
    }

    // MARK: Footer

    private var footer: some View {
        let watermark = "chunithmd"
        let iconSize: CGFloat = 16
        let iconGap: CGFloat = 6
        let groupLeft = (Self.canvasWidth - (iconSize + iconGap + PosterMetrics.width(watermark, size: 12, weight: .regular))) / 2
        return ZStack(alignment: .topLeading) {
            baselineText("●  \(versionText)", size: 11, weight: .bold, color: palette.secondary, baseline: 27)
                .frame(width: Self.canvasWidth, alignment: .center)
            if let appIcon {
                // The bundled mark is transparent, so draw it on the light plate the launcher icon has;
                // that keeps the footer group readable on the dark palette, like Android's adaptive icon.
                ZStack {
                    RoundedRectangle(cornerRadius: 4, style: .circular).fill(.white)
                    appIcon.resizable().scaledToFit().frame(width: 11, height: 11)
                }
                .frame(width: iconSize, height: iconSize)
                .offset(x: groupLeft, y: 36)
            }
            baselineText(watermark, size: 12, weight: .regular, color: palette.subtle, baseline: 49)
                .offset(x: groupLeft + iconSize + iconGap)
        }
        .frame(width: Self.canvasWidth, height: BestTablePosterLayout.footerHeight, alignment: .topLeading)
    }

    // MARK: Drawing helpers

    private func baselineText(_ value: String, size: CGFloat, weight: Font.Weight, color: Color, baseline: CGFloat) -> some View {
        Text(value)
            .font(.system(size: size, weight: weight))
            .foregroundStyle(color)
            .offset(y: baselineOffset(baseline, size: size, weight: weight))
    }

    /// Distance from the top of a single-line text box to its target baseline.
    private func baselineOffset(_ baseline: CGFloat, size: CGFloat, weight: Font.Weight) -> CGFloat {
        baseline - PosterMetrics.ascender(size: size, weight: weight)
    }

    /// The translucent card surface behind a jacket: 15% of the difficulty color over the page.
    private func composite(_ foreground: Int64, over background: Int64) -> Color {
        let alpha = 0.15
        let mix = { (shift: Int64) -> Double in
            let front = Double((foreground >> shift) & 0xFF)
            let back = Double((background >> shift) & 0xFF)
            return (front * alpha + back * (1 - alpha)) / 255
        }
        return Color(.sRGB, red: mix(16), green: mix(8), blue: mix(0), opacity: 1)
    }
}

// MARK: - Formatting

private func posterRating(_ value: Double) -> String { String(format: "%.2f", value) }

private func posterScore(_ value: Int) -> String {
    // Fixed en_US grouping keeps the export reading "1,009,512" like Android's `%,d`, whatever the device locale.
    value.formatted(.number.grouping(.automatic).locale(Locale(identifier: "en_US")))
}

// MARK: - Font metrics

/// Font metrics used to place text on the same baselines as the Android canvas.
private enum PosterMetrics {
    static func font(size: CGFloat, weight: Font.Weight) -> CTFont {
        let base = CTFontCreateUIFontForLanguage(.system, size, nil)
            ?? CTFontCreateWithName("Helvetica" as CFString, size, nil)
        let traits = [kCTFontWeightTrait: weightTrait(weight)] as CFDictionary
        let attributes = [kCTFontTraitsAttribute: traits] as CFDictionary
        return CTFontCreateCopyWithAttributes(base, size, nil, CTFontDescriptorCreateWithAttributes(attributes))
    }

    static func ascender(size: CGFloat, weight: Font.Weight) -> CGFloat {
        CTFontGetAscent(font(size: size, weight: weight))
    }

    static func width(_ value: String, size: CGFloat, weight: Font.Weight) -> CGFloat {
        let attributes = [kCTFontAttributeName: font(size: size, weight: weight)] as CFDictionary
        guard let string = CFAttributedStringCreate(nil, value as CFString, attributes) else { return 0 }
        return CGFloat(CTLineGetTypographicBounds(CTLineCreateWithAttributedString(string), nil, nil, nil))
    }

    private static func weightTrait(_ weight: Font.Weight) -> CGFloat {
        switch weight {
        case .ultraLight: -0.8
        case .thin: -0.6
        case .light: -0.4
        case .regular: 0
        case .medium: 0.23
        case .semibold: 0.3
        case .bold: 0.4
        case .heavy: 0.56
        case .black: 0.62
        default: 0
        }
    }
}
