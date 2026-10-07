import Foundation
import Security
import Shared

nonisolated final class RhythmetaKeychain: RhythmetaSecretStore {
    private let service = "org.rhythmeta.chunithmd.account"
    private func query(_ key: String) -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: key]
    }
    func read(key: String) throws -> StoredSecret {
        var request = query(key)
        request[kSecReturnData as String] = true; request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(request as CFDictionary, &result)
        if status == errSecItemNotFound { return StoredSecret(value: nil) }
        guard status == errSecSuccess, let data = result as? Data else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
        return StoredSecret(value: String(data: data, encoding: .utf8))
    }
    func write(key: String, value: String?) throws {
        let request = query(key)
        guard let value else {
            let status = SecItemDelete(request as CFDictionary)
            guard status == errSecSuccess || status == errSecItemNotFound else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
            return
        }
        let attributes: [String: Any] = [kSecValueData as String: Data(value.utf8), kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
        var status = SecItemUpdate(request as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound { status = SecItemAdd(request.merging(attributes) { _, new in new } as CFDictionary, nil) }
        guard status == errSecSuccess else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
    }
}

nonisolated final class RhythmetaSnapshotFiles: SnapshotFiles {
    private let directory = URL.applicationSupportDirectory.appending(path: "RhythmetaPersonal", directoryHint: .isDirectory)
    private func url(_ name: String) throws -> URL {
        guard ["personal.pb.gz", "restore-pending.pb.gz"].contains(name) else { throw CocoaError(.fileReadInvalidFileName) }
        return directory.appending(path: name)
    }
    func read(name: String) throws -> SnapshotFile {
        let source = try url(name)
        guard FileManager.default.fileExists(atPath: source.path) else { return SnapshotFile(bytes: nil) }
        let size = try source.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
        guard size <= 64 * 1024 * 1024 else { throw CocoaError(.fileReadCorruptFile) }
        let data = try Data(contentsOf: source)
        let bytes = KotlinByteArray(size: Int32(data.count))
        for (index, byte) in data.enumerated() { bytes.set(index: Int32(index), value: Int8(bitPattern: byte)) }
        return SnapshotFile(bytes: bytes)
    }
    func write(name: String, bytes: KotlinByteArray) throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var excluded = directory; var values = URLResourceValues(); values.isExcludedFromBackup = true
        try excluded.setResourceValues(values)
        let data = Data((0..<Int(bytes.size)).map { UInt8(bitPattern: bytes.get(index: Int32($0))) })
        try data.write(to: url(name), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
    func delete(name: String) throws {
        let target = try url(name)
        if FileManager.default.fileExists(atPath: target.path) { try FileManager.default.removeItem(at: target) }
    }
}
