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
