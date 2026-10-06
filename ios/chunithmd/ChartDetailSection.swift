import SwiftUI

struct ChartDetailSection: View {
    let sheet: CatalogSongViewData.Sheet
    let rating: Double?

    var body: some View {
        let tint = difficultyStyle(sheet.difficulty, type: sheet.type)
        VStack(spacing: 16) {
            if let notes = sheet.noteCounts { ChartNotesSection(notes: notes) }
            if sheet.type.lowercased() != "we", let constant = sheet.internalLevelValue ?? sheet.levelValue, constant > 0 {
                ChartRatingSection(constant: constant, rating: rating)
            }
            if let total = sheet.noteCounts?.total, total > 0 {
                ChartToleranceSection(total: total, tint: tint)
            }
        }
    }
}
