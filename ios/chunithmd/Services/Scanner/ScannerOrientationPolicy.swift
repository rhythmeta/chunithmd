import UIKit

@MainActor final class ScannerOrientationPolicy: NSObject, UIApplicationDelegate {
    private static var scanning = false

    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        Self.scanning ? .portrait : (UIDevice.current.userInterfaceIdiom == .pad ? .all : .allButUpsideDown)
    }

    static func setScanning(_ value: Bool) {
        scanning = value
        for scene in UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }) {
            scene.windows.first(where: \.isKeyWindow)?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
            if value { scene.requestGeometryUpdate(.iOS(interfaceOrientations: .portrait)) }
        }
    }
}
