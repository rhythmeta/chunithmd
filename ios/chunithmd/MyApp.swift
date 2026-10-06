import SwiftUI

@main struct MyApp: App {
    init() {
        AppLocalization.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
