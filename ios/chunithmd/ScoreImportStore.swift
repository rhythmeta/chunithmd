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
        var phase: String?
        var eligible = false
        var page = 0
        var totalPages = 0
    }
    var states: [String: Provider] = [:]
    var bridge: NativeImportBridge?
    var error: String?
    private var profileID: String?
    private var server: String?
    private var bundle: CatalogBundle?
    private var generation = UUID()

    @discardableResult
    func start(bundle: CatalogBundle, profile: PersonalSnapshot.Profile) -> Bool {
        if bridge != nil, profileID == profile.id, server == profile.server, self.bundle === bundle { return false }
        close()
        profileID = profile.id
        server = profile.server
        self.bundle = bundle
        let generation = self.generation
        let bridge = NativeImportBridge(secrets: RhythmetaKeychain(), files: RhythmetaSnapshotFiles(), bundle: bundle)
        self.bridge = bridge
        bridge.observe { [weak self] json in
            Task { @MainActor in
                guard let self, self.generation == generation else { return }
                do { self.states = try JSONDecoder().decode([String: Provider].self, from: Data(json.utf8)) }
                catch { self.error = error.localizedDescription }
            }
        }
        bridge.selectProfile(id: profile.id, server: profile.server)
        return true
    }
    func close() {
        generation = UUID()
        bridge?.close()
        bridge = nil
        bundle = nil
        states = [:]
        error = nil
    }
}
