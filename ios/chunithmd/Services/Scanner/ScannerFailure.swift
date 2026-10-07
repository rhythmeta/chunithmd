import Foundation

nonisolated enum ScannerFailure: Error {
    case cameraUnavailable
    case invalidImage, modelMissing, modelContract, noFields
}
