import SwiftUI

/// Shared proportions and surfaces from maimaid's native iOS interface.
enum AppTheme {
    static let page = Color("PageBackground")
    static let surface = Color("CardBackground")
    static let border = Color.primary.opacity(0.05)

    static func serverName(_ server: String) -> String {
        switch server { case "cn": tr("国服"); case "intl": tr("国际服"); default: tr("日服") }
    }
    static func serverColor(_ server: String) -> Color {
        switch server { case "cn": .orange; case "intl": .blue; default: .red }
    }
}
