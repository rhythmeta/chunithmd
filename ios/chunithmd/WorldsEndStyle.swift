import SwiftUI

enum WorldsEndStyle {
    // Match Android's WORLDS_END_GRADIENT_COLORS / WORLDS_END_FILTER_GRADIENT.
    static let colors: [Color] = [
        0xFF65B94A, 0xFFE6BD31, 0xFFE34A47,
        0xFF9A50C9, 0xFF5D5D66, 0xFF4AA8C2,
    ].map { argbColor($0) }

    static let horizontal = LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing)
    static let vertical = LinearGradient(colors: colors, startPoint: .top, endPoint: .bottom)
    static let ring = AngularGradient(colors: colors, center: .center)
}
