import Foundation
import Observation
import Shared
import UIKit

@MainActor @Observable
final class ScannerStore {
    var title = "" { didSet { if !busy && oldValue != title { identityChanged() } } }
    var difficulty = "" { didSet { if !busy && oldValue != difficulty { identityChanged() } } }
    var level = "" { didSet { if !busy && oldValue != level { identityChanged() } } }
    var score = "" { didSet { if !busy && oldValue != score { editChanged() } } }
    var clear = "" { didSet { if !busy && oldValue != clear { editChanged() } } }
    var combo = "" { didSet { if !busy && oldValue != combo { editChanged() } } }
    var selectedKey = "" { didSet { if !busy && oldValue != selectedKey { editChanged() } } }
    private(set) var candidates: [ScannerReviewData.Candidate] = []
    private(set) var preview: UIImage?
    private(set) var rawStatuses = ""
    private(set) var busy = false
    private(set) var saved = false
    var error: String?
    private let recognizer = CoreMLScoreRecognizer()
    private var generation = UUID()

    func reset() {
        generation = UUID(); title = ""; difficulty = ""; level = ""; score = ""
        clear = ""; combo = ""; selectedKey = ""; candidates = []; preview = nil
        busy = false; saved = false; rawStatuses = ""; error = nil
    }
    func identityChanged() { candidates = []; selectedKey = ""; saved = false }
    func editChanged() { saved = false }

    func recognize(data: Data, catalog: CatalogBundle, region: String, files: ScannerModelFiles) async {
        reset(); busy = true
        let token = generation
        defer { if generation == token { busy = false } }
        do {
            let capture = try await recognizer.recognize(data: data, region: region, files: files)
            try Task.checkCancellation()
            guard generation == token else { return }
            let json = try NativeScoreScanner.shared.reviewJson(catalog: catalog, observationsJson: capture.observationsJSON, region: region)
            let review = try JSONDecoder().decode(ScannerReviewData.self, from: Data(json.utf8))
            title = review.fields.title; difficulty = review.fields.difficulty; level = review.fields.level
            score = review.parsedScore.map(String.init) ?? review.fields.score
            clear = review.clear; combo = ""
            rawStatuses = review.fields.clear + " / " + review.fields.combo
            preview = UIImage(data: capture.previewData)
            candidates = review.candidates; selectedKey = review.recommendedKey ?? ""
        } catch is CancellationError { }
        catch {
            guard generation == token, !Task.isCancelled else { return }
            switch error {
            case ScannerFailure.noFields: self.error = tr("未找到成绩字段，请选择清晰的单张成绩图。")
            case ScannerFailure.modelMissing, ScannerFailure.modelContract: self.error = tr("识别模型不可用，请检查模型更新后重试。")
            default: self.error = tr("识别失败") + ": " + error.localizedDescription
            }
        }
    }

    func match(catalog: CatalogBundle, region: String) {
        let json = NativeScoreScanner.shared.matchJson(catalog: catalog, title: title, difficulty: difficulty, level: level, region: region)
        do {
            let review = try JSONDecoder().decode(ScannerReviewData.self, from: Data(json.utf8))
            candidates = review.candidates; selectedKey = review.recommendedKey ?? ""; error = nil
        } catch { self.error = error.localizedDescription }
    }

    func save(catalog: CatalogStore, personal: PersonalStore) {
        guard !saved, let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else { return }
        do {
            try personal.bridge.saveScannedScore(bundle: bundle, profileId: profile.id, region: profile.server,
                chartKey: selectedKey, scoreText: score, clear: clear, combo: combo)
            saved = true; error = nil; personal.reload(catalog: catalog)
        } catch { self.error = error.localizedDescription }
    }
}
