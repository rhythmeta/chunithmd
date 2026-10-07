import SwiftUI

@main struct MyApp: App {
    @UIApplicationDelegateAdaptor(ScannerOrientationPolicy.self) private var orientationPolicy
    init() {
        AppLocalization.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
