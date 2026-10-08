import Foundation
import Observation
import Shared

@MainActor
@Observable
final class ScannerModelController {
    private(set) var state = ScannerModelViewState()
    @ObservationIgnored private var bridge: ScannerModelBridge?
    @ObservationIgnored private var started = false

    init() {
        do {
            let support = URL.applicationSupportDirectory.appending(path: "scanner-models", directoryHint: .isDirectory)
            try FileManager.default.createDirectory(at: support, withIntermediateDirectories: true)
            var excluded = support
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            try excluded.setResourceValues(values)
            bridge = ScannerModelBridge(directory: support.path)
        } catch {
            state.stage = "failed"
            state.error = error.localizedDescription
        }
    }

    func start() {
        guard !started else { return }
        started = true
        bridge?.observe { @Sendable [weak self] json in
            Task { @MainActor [weak self] in
                guard let self, let state = try? JSONDecoder().decode(ScannerModelViewState.self, from: Data(json.utf8)) else { return }
                self.state = state
            }
        }
        bridge?.prepare()
    }
    func check() { bridge?.check() }
    func download() { bridge?.download() }
    func cancel() { bridge?.cancelDownload() }
    func files() throws -> ScannerModelFiles {
        guard let json = bridge?.snapshotJson() else { throw ScannerFailure.modelMissing }
        return try JSONDecoder().decode(ScannerModelFiles.self, from: Data(json.utf8))
    }
    isolated deinit { bridge?.close() }
}
