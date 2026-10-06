import Foundation
import Observation
import Shared

@MainActor @Observable
final class ScoreImportStore {
    struct Provider: Decodable {
        var connected = false
        var busy = false
        var url: String?
        var code: String?
        var error: String?
        var result: String?
    }
    var states: [String: Provider] = [:]
    var bridge: NativeImportBridge?
    var error: String?
    func start(bundle: CatalogBundle, profile: PersonalSnapshot.Profile) {
        bridge?.close()
        let bridge = NativeImportBridge(secrets: RhythmetaKeychain(), files: RhythmetaSnapshotFiles(), bundle: bundle)
        self.bridge = bridge
        bridge.observe { [weak self] json in
            Task { @MainActor in
                do { self?.states = try JSONDecoder().decode([String: Provider].self, from: Data(json.utf8)) }
                catch { self?.error = error.localizedDescription }
            }
        }
        bridge.selectProfile(id: profile.id, server: profile.server)
    }
    func close() { bridge?.close(); bridge = nil }
}
