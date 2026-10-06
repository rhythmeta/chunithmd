import Foundation

struct RhythmetaViewState: Decodable {
    struct User: Decodable { let handle: String; let email: String }
    struct Backup: Decodable, Identifiable { let id: String; let deviceName: String; let committedAt: String; let profileCount: Int; let size: Int }
    var user: User?
    var backups: [Backup] = []
    var busy = false
    var ready = false
    var error: String?
    var message: String?
}

