import Foundation
import Shared

/// Use the same source keys, language resolution, and formatting as the Android client.
enum AppLocalization {
    static func configure() {
        AppStrings.shared.language = AppLanguage.companion.resolve(systemTags: Locale.preferredLanguages)
    }
}

nonisolated func tr(_ key: String, _ arguments: Any...) -> String {
    AppStrings.shared.text(key: key, arguments: arguments.map { String(describing: $0) })
}
