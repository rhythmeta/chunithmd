import AVFoundation
import SwiftUI

struct ScannerCameraPreview: UIViewRepresentable {
    let enabled: Bool
    let analyzing: Bool
    let landscape: Int
    let onFrame: @MainActor @Sendable (Data) async -> Void
    let onError: @MainActor @Sendable () -> Void

    final class Preview: UIView {
        let capture = ScannerCameraCapture()
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
        override init(frame: CGRect) {
            super.init(frame: frame)
            previewLayer.session = capture.session
            previewLayer.videoGravity = .resizeAspectFill
        }
        required init?(coder: NSCoder) { fatalError("init(coder:) is unsupported") }
        override func layoutSubviews() {
            super.layoutSubviews()
            if let connection = previewLayer.connection, connection.isVideoRotationAngleSupported(90) {
                connection.videoRotationAngle = 90
            }
        }
    }
    func makeUIView(context: Context) -> Preview { Preview() }
    func updateUIView(_ view: Preview, context: Context) {
        view.capture.update(enabled: enabled, analyzing: analyzing, landscape: landscape, onFrame: onFrame, onError: onError)
    }
    static func dismantleUIView(_ view: Preview, coordinator: ()) { view.capture.stop() }
}
