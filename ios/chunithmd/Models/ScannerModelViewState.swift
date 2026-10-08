import Foundation

struct ScannerModelViewState: Decodable {
    var stage = "loading"
    var usable = false
    var downloadedBytes: Int64 = 0
    var totalBytes: Int64 = 0
    var offline = false
    var error: String?
}
