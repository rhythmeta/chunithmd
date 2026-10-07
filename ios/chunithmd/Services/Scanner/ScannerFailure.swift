import Foundation

nonisolated enum ScannerFailure: Error {
    case invalidImage, modelMissing, modelContract, noFields
}
