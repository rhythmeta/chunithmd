import Foundation
import Observation
import Photos
import Shared
import UIKit

@MainActor @Observable
final class ScannerStore {
    private(set) var result: ScannerResult?
    private(set) var songResult: ScannerSongPresentation.Match?
    private(set) var preview: UIImage?
    private(set) var busy = false
    private(set) var photoMode = false
    private(set) var savingPhoto = false
    private(set) var detectedBoxes: [ScannerDetectedBox] = []
    private(set) var detectedImageSize = CGSize.zero
    private(set) var detectedOrientation = ScannerPhysicalOrientation.portrait
    var error: String?
    var feedback: String?
    private let recognizer = CoreMLScoreRecognizer()
    private let matching = NativeLiveScoreScanner()
    private let songMatching = NativeLiveSongScanner()
    private var generation = UUID()
    private var inFlight = false

    func reset() {
        invalidate()
        photoMode = false; preview = nil; result = nil; songResult = nil; error = nil; feedback = nil
    }

    func invalidate() {
        generation = UUID()
        detectedBoxes = []; detectedImageSize = .zero
    }
    func orientationChanged() { invalidate(); result = nil; songResult = nil; error = nil }
    func beginPhoto() { reset(); photoMode = true; busy = true }

    func recognize(data: Data, catalog: CatalogBundle, region: String, files: ScannerModelFiles, live: Bool,
                   orientation: ScannerPhysicalOrientation = .portrait) async {
        // Photo imports wait for any in-flight camera recognition to finish on the actor.
        guard !live || (!inFlight && !photoMode) else { return }
        if !live { beginPhoto(); preview = UIImage(data: data) }
        let token = generation
        inFlight = true
        defer { inFlight = false; if generation == token { busy = false } }
        do {
            var songMode = live && orientation == .portrait
            let capture: ScannerCapture
            if live {
                capture = try await recognizer.recognize(data: data, region: region, files: files, songMode: songMode)
            } else {
                // Imported photos are classified by their fields, never by the shape of the photo.
                let scoreCapture: ScannerCapture?
                do { scoreCapture = try await recognizer.recognize(data: data, region: region, files: files) }
                catch ScannerFailure.noFields { scoreCapture = nil }
                let hasScore = scoreCapture.map { Self.hasScoreFields($0.observationsJSON) } ?? false
                if hasScore, let scoreCapture { capture = scoreCapture }
                else {
                    songMode = true
                    capture = try await recognizer.recognize(data: data, region: region, files: files, songMode: true)
                }
            }
            try Task.checkCancellation()
            guard generation == token else { return }
            detectedBoxes = capture.boxes
            detectedImageSize = capture.imageSize
            detectedOrientation = orientation
            if !live { preview = UIImage(data: capture.previewData) }
            if songMode {
                let json = try await songMatching.reviewJson(catalog: catalog, observationsJson: capture.observationsJSON,
                    region: region, live: live, session: token.uuidString)
                try Task.checkCancellation()
                guard generation == token else { return }
                let state = try JSONDecoder().decode(ScannerSongPresentation.self, from: Data(json.utf8))
                if state.accepted { songResult = state.match; result = nil; error = nil }
                else if state.shouldClear || !live {
                    songResult = nil
                    if !live { error = tr("未识别到匹配歌曲，请重新扫描。") }
                }
                return
            }
            let json = try await matching.reviewJson(catalog: catalog, observationsJson: capture.observationsJSON, region: region, live: live, session: token.uuidString)
            try Task.checkCancellation()
            guard generation == token else { return }
            let presentation = try JSONDecoder().decode(ScannerPresentation.self, from: Data(json.utf8))
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
            detectedBoxes = []; detectedImageSize = .zero
            if live, case ScannerFailure.noFields = error {
                // Count empty frames too, so an old card disappears when the camera moves away.
                if orientation == .portrait {
                    if let json = try? await songMatching.reviewJson(catalog: catalog, observationsJson: "[]", region: region,
                        live: true, session: token.uuidString),
                       let state = try? JSONDecoder().decode(ScannerSongPresentation.self, from: Data(json.utf8)),
                       generation == token, state.shouldClear { songResult = nil }
                    return
                }
                if let json = try? await matching.reviewJson(catalog: catalog, observationsJson: "[]", region: region, live: true, session: token.uuidString),
                   let state = try? JSONDecoder().decode(ScannerPresentation.self, from: Data(json.utf8)), generation == token, state.shouldClear { result = nil }
                return
            }
            switch error {
            case ScannerFailure.noFields: self.error = tr("未识别到匹配歌曲，请重新扫描。")
            case ScannerFailure.modelMissing, ScannerFailure.modelContract: self.error = tr("识别模型不可用，请检查模型更新后重试。")
            default: self.error = tr("识别失败") + ": " + error.localizedDescription
            }
        }
    }

    private static func hasScoreFields(_ json: String) -> Bool {
        guard let rows = try? JSONSerialization.jsonObject(with: Data(json.utf8)) as? [[String: Any]] else { return false }
        let score = rows.first { $0["field"] as? String == "score" }?["text"] as? String ?? ""
        let difficulty = rows.first { $0["field"] as? String == "difficulty" }?["text"] as? String ?? ""
        return ScoreScanner.shared.parseScore(raw: score) != nil && ScoreScanner.shared.difficulty(raw: difficulty) != nil
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
