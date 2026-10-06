import SwiftUI
import Shared

struct VersionBadge: View {
    let version: String
    let dark: Bool

    var body: some View {
        let palette = VersionPalette.shared.forVersion(version: version, dark: dark)
        Text(version.replacingOccurrences(of: " PLUS", with: " +"))
            .font(.system(size: 9, weight: .bold))
            .lineLimit(1)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .foregroundStyle(argbColor(dark ? palette.darkForeground : palette.lightForeground))
            .background(argbColor(dark ? palette.darkBackground : palette.lightBackground), in: RoundedRectangle(cornerRadius: 4))
    }
}

