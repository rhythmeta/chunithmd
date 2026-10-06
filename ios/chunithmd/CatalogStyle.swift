import SwiftUI

let difficultyNames = ["basic", "advanced", "expert", "master", "ultima", "world's end"]

func downloadProgressText(_ progress: CatalogSyncProgressViewData) -> String {
    let bytes = progress.totalBytes.map {
        "\(formatByteCount(progress.downloadedBytes)) / \(formatByteCount($0))"
    } ?? formatByteCount(progress.downloadedBytes)
    let speed = progress.bytesPerSecond > 0
        ? " · \(formatByteCount(progress.bytesPerSecond))/s"
        : ""
    let items = progress.totalItems > 0
        ? " · \(progress.completedItems)/\(progress.totalItems)"
        : ""
    return "\(bytes)\(speed)\(items)"
}

func formatByteCount(_ bytes: Int64) -> String {
    let units = ["B", "KB", "MB", "GB"]
    var value = Double(max(bytes, 0))
    var unit = 0
    while value >= 1024 && unit < units.count - 1 {
        value /= 1024
        unit += 1
    }
    return unit == 0 ? "\(Int(value)) \(units[unit])" : String(format: "%.1f %@", value, units[unit])
}

let ultimaGradient = LinearGradient(
    gradient: Gradient(stops: [
        .init(color: Color(red: 0.035, green: 0.035, blue: 0.047), location: 0),
        .init(color: Color(red: 0.035, green: 0.035, blue: 0.047), location: 0.2),
        .init(color: Color(red: 0.89, green: 0.075, blue: 0.27), location: 0.43),
        .init(color: Color(red: 0.89, green: 0.075, blue: 0.27), location: 0.55),
        .init(color: Color(red: 0.035, green: 0.035, blue: 0.047), location: 0.64),
        .init(color: Color(red: 0.035, green: 0.035, blue: 0.047), location: 0.83),
        .init(color: Color(red: 0.89, green: 0.075, blue: 0.27), location: 1),
    ]),
    startPoint: UnitPoint(x: 0, y: 1),
    endPoint: UnitPoint(x: 1, y: 0),
)

func difficultyStyle(_ difficulty: String, type: String? = nil, vertical: Bool = false) -> AnyShapeStyle {
    if type?.lowercased() == "we" || ["we", "world's end"].contains(difficulty.lowercased()) {
        return AnyShapeStyle(vertical ? WorldsEndStyle.vertical : WorldsEndStyle.horizontal)
    }
    return switch difficulty.lowercased() {
    case "basic": AnyShapeStyle(Color(red: 0.31, green: 0.68, blue: 0.24))
    case "advanced": AnyShapeStyle(Color(red: 0.83, green: 0.67, blue: 0.12))
    case "expert": AnyShapeStyle(Color(red: 0.83, green: 0.20, blue: 0.20))
    case "master": AnyShapeStyle(Color(red: 0.54, green: 0.27, blue: 0.73))
    case "ultima": AnyShapeStyle(ultimaGradient)
    default: AnyShapeStyle(Color(red: 0.17, green: 0.57, blue: 0.69))
    }
}

func argbColor(_ value: Int64) -> Color {
    let bits = UInt64(bitPattern: value)
    return Color(
        .sRGB,
        red: Double((bits >> 16) & 0xff) / 255,
        green: Double((bits >> 8) & 0xff) / 255,
        blue: Double(bits & 0xff) / 255,
        opacity: Double((bits >> 24) & 0xff) / 255
    )
}
