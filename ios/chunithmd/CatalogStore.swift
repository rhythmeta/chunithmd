import Foundation
import Observation
import Shared

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

struct CatalogSyncProgressViewData: Decodable {
    let stage: String
    let message: String?
    let progress: Double?
    let completedItems: Int
    let totalItems: Int
    let downloadedBytes: Int64
    let totalBytes: Int64?
    let bytesPerSecond: Int64
}

@MainActor
@Observable
final class CatalogStore {
    private let bridge: CatalogBridge
    private let query = CatalogQuery.shared
    private(set) var bundleJson = ""
    private(set) var bundle: CatalogBundle?
    private(set) var allSongs: [CatalogSongViewData] = []
    private var started = false
    private var searchTask: Task<Void, Never>?
    private(set) var songs: [CatalogSongViewData] = []
    private(set) var manifest: StaticManifestViewData?
    private(set) var categories: [String] = []
    private(set) var versions: [String] = []
    private(set) var difficulties = ["basic", "advanced", "expert", "master", "ultima", "world's end"]
    var search = "" { didSet {
        searchTask?.cancel()
        searchTask = Task { [weak self] in
            do { try await Task.sleep(for: .milliseconds(150)) } catch { return }
            self?.updateResults()
        }
    } }
    var sort = "default" { didSet {
        UserDefaults.standard.set(sort, forKey: "catalog.sort")
        updateResults()
    } }
    var ascending = true { didSet {
        UserDefaults.standard.set(ascending, forKey: "catalog.sortAscending")
        updateResults()
    } }
    var selectedCategories = Set<String>() { didSet { updateResults() } }
    var selectedVersions = Set<String>() { didSet { updateResults() } }
    var selectedDifficulties = Set<String>() { didSet { updateResults() } }
    var server = "jp" { didSet { if oldValue != server { updateAllSongs(); updateResults() } } }
    var aliases: [String: [String]] = [:] { didSet { updateResults() } }
    var favorites: [String] = [] { didSet { updateResults() } }
    var favoritesOnly = false { didSet { updateResults() } }
    var hideDeleted = false { didSet { updateResults() } }
    var minLevel = 1.0 { didSet { updateResults() } }
    var maxLevel = 16.0 { didSet { updateResults() } }
    var playableOnly = false { didSet { updateResults() } }
    var syncMessage = tr("正在读取本地目录")
    var errorMessage: String?
    var isSyncing = false
    var updateAvailable = false
    private(set) var syncProgress: CatalogSyncProgressViewData?

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let directory = support.appending(path: "catalog", directoryHint: .isDirectory)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        bridge = CatalogBridge(cacheDirectory: directory.path)
        if let savedSort = UserDefaults.standard.string(forKey: "catalog.sort"),
           ["default", "versionDate", "difficulty"].contains(savedSort) {
            sort = savedSort
        }
        ascending = UserDefaults.standard.object(forKey: "catalog.sortAscending") as? Bool ?? true
    }

    func start() {
        guard !started else { return }; started = true
        if let json = bridge.loadSnapshotJson() {
            install(json)
            checkForUpdate()
        } else {
            refresh()
        }
    }

    func checkForUpdate() {
        isSyncing = true
        errorMessage = nil
        syncProgress = nil
        updateAvailable = false
        syncMessage = tr("正在检查更新")
        bridge.checkForUpdate { @Sendable [weak self] json, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isSyncing = false
                if let error {
                    self.errorMessage = error
                    self.syncMessage = tr("检查更新失败")
                } else if let json, let result = try? JSONDecoder().decode(UpdateCheckViewData.self, from: Data(json.utf8)) {
                    self.updateAvailable = result.updateAvailable
                    self.syncMessage = result.updateAvailable ? tr("发现可用更新") : tr("已是最新版本")
                }
            }
        }
    }

    func jacketURL(for imageName: String) -> URL? {
        guard !imageName.isEmpty else { return nil }
        if let localPath = bridge.localJacketPath(imageName: imageName) {
            return URL(fileURLWithPath: localPath)
        }
        guard let baseURL = manifest?.assets.jacketBaseUrl else { return nil }
        return URL(string: baseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + "/" + imageName.trimmingCharacters(in: CharacterSet(charactersIn: "/")))
    }

    func refresh() {
        isSyncing = true
        errorMessage = nil
        syncProgress = nil
        syncMessage = tr("正在下载歌曲目录")
        bridge.refreshWithProgress(onProgress: { @Sendable [weak self] progressJson in
            Task { @MainActor [weak self] in
                guard let self, let progress = try? JSONDecoder().decode(
                    CatalogSyncProgressViewData.self, from: Data(progressJson.utf8)
                ) else { return }
                self.syncProgress = progress
                self.syncMessage = progress.message ?? self.syncMessage
            }
        }, completion: { @Sendable [weak self] json, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isSyncing = false
                self.syncProgress = nil
                if let error {
                    self.errorMessage = error
                    self.syncMessage = tr("资源更新失败")
                } else if let json {
                    self.install(json)
                    self.updateAvailable = false
                    self.syncMessage = tr("资源已更新")
                }
            }
        })
    }

    private func install(_ json: String) {
        bundleJson = json
        if let metadataJson = bridge.loadSnapshotMetadataJson() {
            manifest = try? JSONDecoder().decode(StaticManifestViewData.self, from: Data(metadataJson.utf8))
        }
        let bundle = CatalogJson.shared.decodeBundle(source: json)
        self.bundle = bundle
        updateAllSongs()
        categories = query.availableCategories(bundle: bundle)
        versions = query.availableVersions(bundle: bundle)
        updateResults()
    }

    private func updateAllSongs() {
        guard let bundle else { return }
        let json = NativeCatalogQuery.shared.songs(bundle: bundle, server: server)
        allSongs = (try? JSONDecoder().decode([CatalogSongViewData].self, from: Data(json.utf8))) ?? []
    }

    private func updateResults() {
        guard let bundle else { songs = []; return }
        let request: [String: Any] = [
            "search": search, "sort": sort, "ascending": ascending,
            "categories": selectedCategories.sorted(), "versions": selectedVersions.sorted(),
            "difficulties": selectedDifficulties.sorted(),
            "playableOnly": playableOnly, "hideDeleted": hideDeleted,
            "favoritesOnly": favoritesOnly, "favorites": favorites,
            "minLevel": minLevel, "maxLevel": maxLevel, "server": server
        ]
        do {
            let requestData = try JSONSerialization.data(withJSONObject: request)
            let aliasesData = try JSONEncoder().encode(aliases)
            let json = try NativeCatalogQuery.shared.search(bundle: bundle,
                requestJson: String(decoding: requestData, as: UTF8.self), aliasesJson: String(decoding: aliasesData, as: UTF8.self))
            songs = try JSONDecoder().decode([CatalogSongViewData].self, from: Data(json.utf8))
        } catch { errorMessage = error.localizedDescription }
    }
}
