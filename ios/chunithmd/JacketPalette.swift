import SwiftUI
import CoreImage

@MainActor
enum JacketPalette {
    private static let cache = NSCache<NSURL, UIColor>()

    static func color(for url: URL?, dark: Bool) async -> Color? {
        guard let url else { return nil }
        let source: UIColor
        if let cached = cache.object(forKey: url as NSURL) { source = cached }
        else {
            guard let data = try? await load(url), !Task.isCancelled,
                  let image = CIImage(data: data),
                  let average = CIFilter(name: "CIAreaAverage", parameters: [kCIInputImageKey: image, kCIInputExtentKey: CIVector(cgRect: image.extent)])?.outputImage else { return nil }
            var pixel = [UInt8](repeating: 0, count: 4)
            CIContext(options: [.workingColorSpace: NSNull()]).render(average, toBitmap: &pixel, rowBytes: 4,
                bounds: CGRect(x: 0, y: 0, width: 1, height: 1), format: .RGBA8, colorSpace: nil)
            source = UIColor(red: CGFloat(pixel[0]) / 255, green: CGFloat(pixel[1]) / 255, blue: CGFloat(pixel[2]) / 255, alpha: 1)
            cache.countLimit = 100
            cache.setObject(source, forKey: url as NSURL)
        }
        var hue: CGFloat = 0, saturation: CGFloat = 0, brightness: CGFloat = 0, alpha: CGFloat = 0
        source.getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: &alpha)
        return Color(hue: Double(hue),
            saturation: Double(dark ? min(max(saturation * 0.75, 0.2), 0.45) : min(max(saturation * 0.45, 0.08), 0.3)),
            brightness: Double(dark ? min(max(brightness * 0.35, 0.12), 0.28) : min(max(0.88 + (brightness - 0.5) * 0.08, 0.84), 0.94)))
    }

    @concurrent private static func load(_ url: URL) async throws -> Data {
        if url.isFileURL { return try Data(contentsOf: url) }
        return try await URLSession.shared.data(from: url).0
    }
}
