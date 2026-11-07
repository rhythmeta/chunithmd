import UIKit

/// Temporary PNG files handed to the system share sheet.
enum ExportFile {
    /// Encodes `image` into a new temporary PNG and prunes yesterday's exports.
    static func write(_ image: UIImage) throws -> URL {
        guard let bytes = image.pngData() else { throw CocoaError(.fileWriteUnknown) }
        removeExpired()
        let file = URL.temporaryDirectory.appending(path: "chunithmd-\(UUID().uuidString).png")
        try bytes.write(to: file, options: .atomic)
        return file
    }

    /// Share extensions may still read a file after the sheet closes; drop yesterday's exports.
    private static func removeExpired() {
        let files = (try? FileManager.default.contentsOfDirectory(at: .temporaryDirectory, includingPropertiesForKeys: [.contentModificationDateKey])) ?? []
        for file in files where file.lastPathComponent.hasPrefix("chunithmd-") && file.pathExtension == "png" {
            guard let modified = try? file.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate,
                  modified < Date.now.addingTimeInterval(-86_400) else { continue }
            try? FileManager.default.removeItem(at: file)
        }
    }

    @concurrent static func data(from url: URL) async throws -> Data {
        if url.isFileURL { return try Data(contentsOf: url) }
        return try await URLSession.shared.data(from: url).0
    }
}
