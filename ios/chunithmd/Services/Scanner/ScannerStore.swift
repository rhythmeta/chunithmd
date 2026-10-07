import Foundation
import Observation
import Photos
import Shared
import UIKit

@MainActor @Observable
final class ScannerStore {
    private(set) var result: ScannerResult?
    private(set) var preview: UIImage?
    private(set) var busy = false
    private(set) var photoMode = false
    private(set) var savingPhoto = false
    var error: String?
    var feedback: String?
    private let recognizer = CoreMLScoreRecognizer()
    private let matching = NativeLiveScoreScanner()
    private var generation = UUID()
    private var inFlight = false

    func reset() {
        invalidate()
        photoMode = false; preview = nil; result = nil; error = nil; feedback = nil
    }

    func invalidate() { generation = UUID() }
    func beginPhoto() { reset(); photoMode = true; busy = true }

    func recognize(data: Data, catalog: CatalogBundle, region: String, files: ScannerModelFiles, live: Bool) async {
        // Photo imports wait for any in-flight camera recognition to finish on the actor.
        guard !live || (!inFlight && !photoMode) else { return }
        if !live { beginPhoto(); preview = UIImage(data: data) }
        let token = generation
        inFlight = true
        defer { inFlight = false; if generation == token { busy = false } }
        do {
            let capture = try await recognizer.recognize(data: data, region: region, files: files)
            try Task.checkCancellation()
            guard generation == token else { return }
            let json = try await matching.reviewJson(catalog: catalog, observationsJson: capture.observationsJSON, region: region, live: live, session: token.uuidString)
            try Task.checkCancellation()
            guard generation == token else { return }
            let presentation = try JSONDecoder().decode(ScannerPresentation.self, from: Data(json.utf8))
            if !live { preview = UIImage(data: capture.previewData) }
            if presentation.accepted, let match = presentation.match, let score = presentation.review.parsedScore {
                result = ScannerResult(match: match, score: score, clear: presentation.review.clear, photoData: capture.previewData)
                error = nil
            } else if presentation.shouldClear || !live {
                result = nil
                if !live { error = tr("未识别到匹配谱面，请重新扫描。") }
            }
        } catch is CancellationError { }
        catch {
            guard generation == token, !Task.isCancelled else { return }
            if live, case ScannerFailure.noFields = error {
                // Count empty frames too, so an old card disappears when the camera moves away.
                if let json = try? await matching.reviewJson(catalog: catalog, observationsJson: "[]", region: region, live: true, session: token.uuidString),
                   let state = try? JSONDecoder().decode(ScannerPresentation.self, from: Data(json.utf8)), generation == token, state.shouldClear { result = nil }
                return
            }
            switch error {
            case ScannerFailure.noFields: self.error = tr("未找到成绩字段，请选择清晰的单张成绩图。")
            case ScannerFailure.modelMissing, ScannerFailure.modelContract: self.error = tr("识别模型不可用，请检查模型更新后重试。")
            default: self.error = tr("识别失败") + ": " + error.localizedDescription
            }
        }
    }

    func savePhoto() async {
        guard !savingPhoto, let data = result?.photoData else { return }
        savingPhoto = true
        defer { savingPhoto = false }
        let permission = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard permission == .authorized || permission == .limited else { feedback = tr("请在设置中允许保存照片。"); return }
        do {
            try await PHPhotoLibrary.shared().performChanges {
                PHAssetCreationRequest.forAsset().addResource(with: .photo, data: data, options: nil)
            }
            feedback = tr("照片已保存")
        } catch { feedback = tr("照片保存失败") }
    }
}
