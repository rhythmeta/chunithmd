import SwiftUI

/// Player Rating gradient shared by the summary badge and the exported Best-table image.
///
/// Mirrors the Android `playerRatingColors` palette so both clients render the same colors
/// for on-screen badges and generated images.
func playerRatingColors(_ rating: Double) -> [Color] {
    switch rating {
    case 16...: [0xFFFF5E5E, 0xFFFFF75E, 0xFF5EFF5E, 0xFF5EBAFF, 0xFFBA5EFF].map(argbColor)
    case 15.25..<16: [0xFFCCCCCC, 0xFFFFFFFF, 0xFFCCCCCC].map(argbColor)
    case 14.5..<15.25: [0xFFFFD700, 0xFFFFA500].map(argbColor)
    default: [0xFFFF9500, 0xFFFF9500].map(argbColor)
    }
}

/// Single-chart Rating style used by the Android Best-table row.
func ratingTierStyle(_ rating: Double) -> AnyShapeStyle {
    switch rating {
    case 16...:
        return AnyShapeStyle(LinearGradient(
            colors: [0xFFFF5E5E, 0xFFFFBA5E, 0xFFFFF75E, 0xFF5EFF5E, 0xFF5EBAFF, 0xFFBA5EFF, 0xFFFF5EBA].map(argbColor),
            startPoint: .leading,
            endPoint: .trailing
        ))
    case 15.25..<16:
        return AnyShapeStyle(LinearGradient(
            colors: [0xFFD3D3D3, 0xFFFFFFFF, 0xFFD3D3D3].map(argbColor),
            startPoint: .leading,
            endPoint: .trailing
        ))
    case 14.5..<15.25:
        return AnyShapeStyle(LinearGradient(
            colors: [0xFFFFD700, 0xFFFFA500].map(argbColor),
            startPoint: .top,
            endPoint: .bottom
        ))
    default:
        let color: Color = switch rating {
        case 13.25..<14.5: argbColor(0xFFC0C0C0)
        case 12..<13.25: argbColor(0xFFCD7F32)
        case 10..<12: argbColor(0xFFD084FF)
        case 7..<10: argbColor(0xFFFF5E5E)
        case 4..<7: argbColor(0xFFFF9500)
        default: argbColor(0xFF46D246)
        }
        return AnyShapeStyle(color)
    }
}
