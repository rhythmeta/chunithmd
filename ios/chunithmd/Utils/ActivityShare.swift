import UIKit

/// Presents the system activity sheet from the foreground window.
///
/// Exported images are shared straight from the toolbar, so there is no in-app preview
/// container: the system share sheet is the only surface the user sees.
@MainActor
enum ActivityShare {
    /// Returns whether the sheet could be presented, so callers can surface a failure instead of
    /// leaving the export silently unshared.
    @discardableResult
    static func present(items: [Any]) -> Bool {
        guard let presenter = topViewController() else { return false }
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        if let popover = controller.popoverPresentationController {
            // iPad and Mac Catalyst present the sheet as a popover anchored to the source view.
            popover.sourceView = presenter.view
            popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 0, height: 0)
            popover.permittedArrowDirections = []
        }
        presenter.present(controller, animated: true)
        return true
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        guard let window = scene?.windows.first(where: \.isKeyWindow) ?? scene?.windows.first else { return nil }
        var top = window.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
