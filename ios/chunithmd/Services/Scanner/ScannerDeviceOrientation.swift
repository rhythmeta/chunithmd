import CoreMotion
import Foundation
import Observation
import UIKit

@MainActor @Observable
final class ScannerDeviceOrientation {
    private(set) var orientation = ScannerPhysicalOrientation.portrait
    @ObservationIgnored private let motion = CMMotionManager()
    @ObservationIgnored private var session: UUID?

    /// Owned by the visible, active scanner's task; cancellation stops sensor sampling.
    func track() async {
        stop()
        let token = UUID()
        session = token
        orientation = .portrait
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        if motion.isDeviceMotionAvailable {
            motion.deviceMotionUpdateInterval = 0.1
            motion.startDeviceMotionUpdates()
        } else if motion.isAccelerometerAvailable {
            motion.accelerometerUpdateInterval = 0.1
            motion.startAccelerometerUpdates()
        }
        defer { if session == token { stop() } }
        while !Task.isCancelled && session == token {
            if let gravity = motion.deviceMotion?.gravity {
                orientation = orientation.updated(gravityX: gravity.x, gravityY: gravity.y)
            } else if let acceleration = motion.accelerometerData?.acceleration {
                orientation = orientation.updated(gravityX: acceleration.x, gravityY: acceleration.y)
            } else {
                // Simulator / unavailable motion data. UIKit is a fallback, never the
                // authority over a sensor sample when the user has rotation lock enabled.
                switch UIDevice.current.orientation {
                case .landscapeLeft: orientation = .landscapeLeft
                case .landscapeRight: orientation = .landscapeRight
                case .portrait, .portraitUpsideDown: orientation = .portrait
                default: break
                }
            }
            do { try await Task.sleep(for: .milliseconds(100)) }
            catch { return }
        }
    }

    private func stop() {
        motion.stopDeviceMotionUpdates()
        motion.stopAccelerometerUpdates()
        if session != nil { UIDevice.current.endGeneratingDeviceOrientationNotifications() }
        session = nil
    }
}
