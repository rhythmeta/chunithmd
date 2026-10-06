import Foundation
import Observation
import Shared

@MainActor @Observable
final class RhythmetaAccountStore {
    let bridge = RhythmetaBridge(secrets: RhythmetaKeychain(), files: RhythmetaSnapshotFiles(), clientVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1")
    var state = RhythmetaViewState()
    var loginError: String?
    var community = CommunityViewState()
    init() {
        bridge.observe { [weak self] json in
            Task { @MainActor in
                do { self?.state = try JSONDecoder().decode(RhythmetaViewState.self, from: Data(json.utf8)) }
                catch { self?.loginError = error.localizedDescription }
            }
        }
        bridge.observeCommunity(directory: URL.applicationSupportDirectory.appending(path: "community").path) { [weak self] json in
            Task { @MainActor in
                do { self?.community = try JSONDecoder().decode(CommunityViewState.self, from: Data(json.utf8)) }
                catch { self?.loginError = error.localizedDescription }
            }
        }
        bridge.start()
    }
}

