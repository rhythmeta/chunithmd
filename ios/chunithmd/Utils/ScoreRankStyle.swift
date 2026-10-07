import SwiftUI

func scoreRankColor(_ rank: String) -> Color {
    switch rank.uppercased() {
    case "SSS+", "SSS": argbColor(0xFFFFD900)
    case "SS+", "SS": argbColor(0xFFFFBF00)
    case "S+", "S": argbColor(0xFFFF9900)
    case "AAA": argbColor(0xFFCC99FF)
    case "AA": argbColor(0xFF99CCFF)
    case "A": argbColor(0xFF80E680)
    default: .secondary
    }
}
