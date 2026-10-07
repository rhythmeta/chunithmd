@preconcurrency import AVFoundation
import CoreImage
import Foundation
import ImageIO
import os
import UniformTypeIdentifiers

/// AVFoundation mutation and delegate callbacks share one serial queue. The lock protects
/// control flags accessed from SwiftUI and keeps only one frame in the recognition pipeline.
nonisolated final class ScannerCameraCapture: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate, Sendable {
    private struct State {
        let session = AVCaptureSession()
        var configured = false
        var enabled = false
        var analyzing = false
        var landscape: Int = 0
        var processing = false
        var lastFrame = 0.0
        var onFrame: (@MainActor @Sendable (Data, ScannerPhysicalOrientation) async -> Void)?
        var onError: (@MainActor @Sendable () -> Void)?
    }
    private let state = OSAllocatedUnfairLock(uncheckedState: State())
    private let queue = DispatchQueue(label: String(describing: ScannerCameraCapture.self), qos: .userInitiated)
    private let context = CIContext(options: [.useSoftwareRenderer: false])
    var session: AVCaptureSession { state.withLockUnchecked { $0.session } }

    func update(enabled: Bool, analyzing: Bool, landscape: Int,
                onFrame: @escaping @MainActor @Sendable (Data, ScannerPhysicalOrientation) async -> Void,
                onError: @escaping @MainActor @Sendable () -> Void) {
        state.withLockUnchecked {
            $0.enabled = enabled; $0.analyzing = analyzing; $0.landscape = landscape
            $0.onFrame = onFrame; $0.onError = onError
        }
        queue.async { [self] in synchronizeSession() }
    }

    func stop() {
        state.withLockUnchecked { $0.enabled = false; $0.analyzing = false; $0.onFrame = nil; $0.onError = nil }
        queue.async { [self] in synchronizeSession() }
    }

    private func synchronizeSession() {
        let enabled = state.withLockUnchecked { $0.enabled }
        if enabled && !state.withLockUnchecked({ $0.configured }) {
            do { try configure() }
            catch {
                let callback = state.withLockUnchecked { $0.onError }
                Task { @MainActor in callback?() }
                return
            }
        }
        if enabled && !session.isRunning { session.startRunning() }
        else if !enabled && session.isRunning { session.stopRunning() }
    }

    private func configure() throws {
        let session = self.session
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .hd1920x1080
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            throw ScannerFailure.cameraUnavailable
        }
        let input = try AVCaptureDeviceInput(device: device)
        let output = AVCaptureVideoDataOutput()
        output.alwaysDiscardsLateVideoFrames = true
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        guard session.canAddInput(input) else { throw ScannerFailure.cameraUnavailable }
        session.addInput(input)
        guard session.canAddOutput(output) else { session.removeInput(input); throw ScannerFailure.cameraUnavailable }
        session.addOutput(output)
        output.setSampleBufferDelegate(self, queue: queue)
        // Keep the native landscape buffer. Physical orientation is applied before inference.
        if let connection = output.connection(with: .video), connection.isVideoRotationAngleSupported(0) {
            connection.videoRotationAngle = 0
        }
        state.withLockUnchecked { $0.configured = true }
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let now = ProcessInfo.processInfo.systemUptime
        let frame = state.withLockUnchecked { state -> (Int, @MainActor @Sendable (Data, ScannerPhysicalOrientation) async -> Void)? in
            guard state.enabled, state.analyzing, state.landscape != 0, !state.processing,
                  now - state.lastFrame >= 0.15, let callback = state.onFrame else { return nil }
            state.processing = true; state.lastFrame = now
            return (state.landscape, callback)
        }
        guard let (orientation, callback) = frame else { return }
        guard let data = autoreleasepool(invoking: { encodeFrame(sampleBuffer, orientation: orientation) }) else {
            state.withLockUnchecked { $0.processing = false }; return
        }
        Task { @MainActor [self] in
            await callback(data, ScannerPhysicalOrientation(rawValue: orientation) ?? .portrait)
            state.withLockUnchecked { $0.processing = false }
        }
    }

    private func encodeFrame(_ sampleBuffer: CMSampleBuffer, orientation: Int) -> Data? {
        guard let buffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return nil }
        let source = CIImage(cvPixelBuffer: buffer).oriented(orientation == 1 ? .up : .down)
        guard let image = context.createCGImage(source, from: source.extent) else { return nil }
        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(data, UTType.jpeg.identifier as CFString, 1, nil) else { return nil }
        CGImageDestinationAddImage(destination, image, [kCGImageDestinationLossyCompressionQuality: 0.92] as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return data as Data
    }
}
