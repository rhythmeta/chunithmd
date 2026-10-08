import CoreML
import CoreGraphics
import Foundation
import ImageIO
import Shared
import UniformTypeIdentifiers
import Vision

/// Serial actor keeps Core ML/Vision and image processing off the UI actor.
actor CoreMLScoreRecognizer {
    private var models: [String: MLModel] = [:]
    private var modelRevision: String?

    func recognize(data: Data, region: String, files: ScannerModelFiles, songMode: Bool = false) throws -> ScannerCapture {
        try Task.checkCancellation()
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 2560,
                kCGImageSourceShouldCacheImmediately: true
              ] as CFDictionary) else { throw ScannerFailure.invalidImage }
        let channels = songMode ? 5 : 10
        let detector = try loadModel(files: files, songMode: songMode)
        let boxed = try letterbox(image)
        guard let constraint = detector.modelDescription.inputDescriptionsByName["image"]?.imageConstraint else {
            throw ScannerFailure.modelContract
        }
        let input = try MLFeatureValue(cgImage: boxed, constraint: constraint)
        let output = try detector.prediction(from: MLDictionaryFeatureProvider(dictionary: ["image": input]))
        guard let name = output.featureNames.first,
              let raw = output.featureValue(for: name)?.multiArrayValue,
              raw.shape.count == 3, raw.shape[0].intValue == 1, raw.shape[1].intValue == channels else {
            throw ScannerFailure.modelContract
        }
        let count = raw.shape[2].intValue
        let values = KotlinFloatArray(size: Int32(channels * count))
        let strides = raw.strides.map(\.intValue)
        if raw.dataType == .float32 {
            let pointer = raw.dataPointer.assumingMemoryBound(to: Float.self)
            for channel in 0..<channels {
                for index in 0..<count { values.set(index: Int32(channel * count + index), value: pointer[channel * strides[1] + index * strides[2]]) }
            }
        } else {
            for channel in 0..<channels {
                for index in 0..<count { values.set(index: Int32(channel * count + index), value: raw[[0, NSNumber(value: channel), NSNumber(value: index)]].floatValue) }
            }
        }
        let detections = songMode
            ? ScoreDetection.shared.decodeTitle(values: values, channels: Int32(channels), count: Int32(count), sourceWidth: Int32(image.width), sourceHeight: Int32(image.height))
            : ScoreDetection.shared.decode(values: values, channels: Int32(channels), count: Int32(count), sourceWidth: Int32(image.width), sourceHeight: Int32(image.height))
        guard !detections.isEmpty else { throw ScannerFailure.noFields }
        var observations: [[String: Any]] = []
        for detection in detections {
            try Task.checkCancellation()
            let b = ScoreDetection.shared.cropBox(detection: detection)
            let rect = CGRect(x: Double(b.x) * Double(image.width), y: Double(b.y) * Double(image.height),
                width: Double(b.width) * Double(image.width), height: Double(b.height) * Double(image.height)).integral
                .intersection(CGRect(x: 0, y: 0, width: image.width, height: image.height))
            guard let crop = image.cropping(to: rect) else { continue }
            let ocrImage = try enlarge(crop)
            let request = VNRecognizeTextRequest()
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false
            request.recognitionLanguages = region == "cn" ? ["zh-Hans", "en-US", "ja-JP"] : ["ja-JP", "en-US", "zh-Hans"]
            request.minimumTextHeight = 0
            try VNImageRequestHandler(cgImage: ocrImage).perform([request])
            let text = (request.results ?? []).sorted { $0.boundingBox.midY > $1.boundingBox.midY }
                .compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
            observations.append(["field": detection.field, "text": text, "confidence": detection.confidence,
                "box": ["x": detection.box.x, "y": detection.box.y, "width": detection.box.width, "height": detection.box.height]])
        }
        try Task.checkCancellation()
        let json = try JSONSerialization.data(withJSONObject: observations)
        let preview = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(preview, UTType.jpeg.identifier as CFString, 1, nil) else { throw ScannerFailure.invalidImage }
        CGImageDestinationAddImage(destination, image, [kCGImageDestinationLossyCompressionQuality: 0.8] as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { throw ScannerFailure.invalidImage }
        let boxes = detections.map { detection in
            let box = detection.box
            return ScannerDetectedBox(field: detection.field,
                rect: CGRect(x: Double(box.x), y: Double(box.y), width: Double(box.width), height: Double(box.height)))
        }
        return ScannerCapture(observationsJSON: String(decoding: json, as: UTF8.self), previewData: preview as Data,
            boxes: boxes, imageSize: CGSize(width: image.width, height: image.height))
    }

    private func loadModel(files: ScannerModelFiles, songMode: Bool) throws -> MLModel {
        let name = songMode ? "SongDetector" : "ScoreDetector"
        if modelRevision != files.revision { models.removeAll(); modelRevision = files.revision }
        if let model = models[name] { return model }
        let directory = URL(filePath: files.directory, directoryHint: .isDirectory)
        let compiled = directory.appending(path: "\(name).mlmodelc", directoryHint: .isDirectory)
        let config = MLModelConfiguration()
        config.computeUnits = .all
        // Compiled caches can become invalid after an OS upgrade; rebuild from verified source.
        if let cached = try? MLModel(contentsOf: compiled, configuration: config) {
            models[name] = cached
            return cached
        }
        let package = directory.appending(path: "\(name).mlpackage", directoryHint: .isDirectory)
        let temporary = try MLModel.compileModel(at: package)
        defer { try? FileManager.default.removeItem(at: temporary) }
        try Task.checkCancellation()
        if FileManager.default.fileExists(atPath: compiled.path) { try FileManager.default.removeItem(at: compiled) }
        try FileManager.default.moveItem(at: temporary, to: compiled)
        let loaded = try MLModel(contentsOf: compiled, configuration: config)
        models[name] = loaded
        return loaded
    }

    private func context(width: Int, height: Int) throws -> CGContext {
        guard let context = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8,
            bytesPerRow: width * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { throw ScannerFailure.invalidImage }
        context.interpolationQuality = .high
        return context
    }

    private func letterbox(_ image: CGImage) throws -> CGImage {
        let n = Int(ScoreDetection.shared.inputSize)
        let fit = ScoreDetection.shared.letterbox(width: Int32(image.width), height: Int32(image.height))
        let ctx = try context(width: n, height: n)
        ctx.setFillColor(CGColor(gray: 114.0 / 255.0, alpha: 1)); ctx.fill(CGRect(x: 0, y: 0, width: n, height: n))
        // CGContext y is bottom-up; shared box coordinates are top-down.
        ctx.draw(image, in: CGRect(x: Int(fit.left), y: n - Int(fit.top) - Int(fit.height), width: Int(fit.width), height: Int(fit.height)))
        guard let output = ctx.makeImage() else { throw ScannerFailure.invalidImage }
        return output
    }

    private func enlarge(_ image: CGImage) throws -> CGImage {
        let scale = max(1, min(4, min(96.0 / Double(image.height), 2300.0 / Double(image.width))))
        let w = Int((Double(image.width) * scale).rounded()), h = Int((Double(image.height) * scale).rounded())
        let ctx = try context(width: w + 24, height: h + 24)
        ctx.setFillColor(CGColor(gray: 1, alpha: 1)); ctx.fill(CGRect(x: 0, y: 0, width: w + 24, height: h + 24))
        ctx.draw(image, in: CGRect(x: 12, y: 12, width: w, height: h))
        guard let result = ctx.makeImage() else { throw ScannerFailure.invalidImage }
        return result
    }
}
