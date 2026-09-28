import Foundation
import Observation
import Shared

struct CatalogSongViewData: Decodable, Identifiable {
    struct Sheet: Decodable {
        let type: String
        let difficulty: String
        let level: String
        let levelValue: Double?
        let regions: [String: Bool]?
    }

    let songId: String
    let title: String
    let artist: String
    let category: String
    let imageName: String
    let version: String?
    let sheets: [Sheet]

    var id: String { songId }
}

struct StaticManifestViewData: Decodable {
    let version: String
    let sha256: String
    let createdAt: String
    let assets: Assets

    struct Assets: Decodable {
        let jacketBaseUrl: String
    }
}

private struct UpdateCheckViewData: Decodable {
    let manifest: StaticManifestViewData
    let updateAvailable: Bool
}

@MainActor
@Observable
final class CatalogStore {
    private let bridge: CatalogBridge
    private let query = CatalogQuery.shared
    private(set) var bundleJson = ""
    private(set) var songs: [CatalogSongViewData] = []
    private(set) var manifest: StaticManifestViewData?
    private(set) var categories: [String] = []
    private(set) var versions: [String] = []
    private(set) var difficulties = ["basic", "advanced", "expert", "master", "ultima", "world's end"]
    private(set) var types: [String] = []
    var search = "" { didSet { updateResults() } }
    var sort = "default" { didSet { updateResults() } }
    var ascending = true { didSet { updateResults() } }
    var selectedCategories = Set<String>() { didSet { updateResults() } }
    var selectedVersions = Set<String>() { didSet { updateResults() } }
    var selectedDifficulties = Set<String>() { didSet { updateResults() } }
    var selectedTypes = Set<String>() { didSet { updateResults() } }
    var playableOnly = false { didSet { updateResults() } }
    var syncMessage = "正在读取本地目录"
    var errorMessage: String?
    var isSyncing = false
    var updateAvailable = false

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let directory = support.appending(path: "catalog", directoryHint: .isDirectory)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        bridge = CatalogBridge(cacheDirectory: directory.path)
    }

    func start() {
        if let json = bridge.loadSnapshotJson() {
            install(json)
            checkForUpdate()
        } else {
            refresh()
        }
    }

    func checkForUpdate() {
        syncMessage = "正在检查更新"
        bridge.checkForUpdate { [weak self] json, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error {
                    self.errorMessage = error
                    self.syncMessage = "检查更新失败"
                } else if let json, let result = try? JSONDecoder().decode(UpdateCheckViewData.self, from: Data(json.utf8)) {
                    self.updateAvailable = result.updateAvailable
                    self.syncMessage = result.updateAvailable ? "发现可用更新" : "已是最新版本"
                }
            }
        }
    }

    func refresh() {
        isSyncing = true
        errorMessage = nil
        syncMessage = "正在下载歌曲目录"
        bridge.refresh { [weak self] json, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isSyncing = false
                if let error {
                    self.errorMessage = error
                    self.syncMessage = "资源更新失败"
                } else if let json {
                    self.install(json)
                    self.updateAvailable = false
                    self.syncMessage = "资源已更新"
                }
            }
        }
    }

    private func install(_ json: String) {
        bundleJson = json
        if let metadataJson = bridge.loadSnapshotMetadataJson() {
            manifest = try? JSONDecoder().decode(StaticManifestViewData.self, from: Data(metadataJson.utf8))
        }
        let bundle = CatalogJson.shared.decodeBundle(source: json)
        categories = query.availableCategories(bundle: bundle)
        versions = query.availableVersions(bundle: bundle)
        types = query.availableTypes(bundle: bundle)
        updateResults()
    }

    private func updateResults() {
        guard !bundleJson.isEmpty else { songs = []; return }
        let json = CatalogQuery.shared.searchAndFilterJson(
            bundleJson: bundleJson,
            search: search,
            sort: sort,
            ascending: ascending,
            categories: selectedCategories.sorted(),
            versions: selectedVersions.sorted(),
            difficulties: selectedDifficulties.sorted(),
            types: selectedTypes.sorted(),
            playableOnly: playableOnly
        )
        songs = (try? JSONDecoder().decode([CatalogSongViewData].self, from: Data(json.utf8))) ?? []
    }
}
