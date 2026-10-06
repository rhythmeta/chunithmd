import Shared
import SwiftUI
import PhotosUI

struct ProfileEditorView: View {
    let profile: PersonalSnapshot.Profile?
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var name = ""
    @State private var title = ""
    @State private var server = "jp"
    @State private var photo: PhotosPickerItem?
    @State private var avatar: UIImage?
    @State private var avatarChanged = false
    @State private var error: String?
    var body: some View {
        let currentAvatar = avatar
        NavigationStack {
            Form {
                PhotosPicker(selection: $photo, matching: .images) {
                    HStack {
                        ProfileAvatarView(image: currentAvatar).frame(width: 64, height: 64)
                        Text(tr("选择头像"))
                    }
                }
                TextField(tr("档案名称"), text: $name)
                TextField(tr("称号"), text: $title)
                Picker(tr("服务器"), selection: $server) { Text(tr("日本")).tag("jp"); Text(tr("国际")).tag("intl"); Text(tr("中国")).tag("cn") }
                if let error { Text(error).foregroundStyle(.red) }
            }
            .navigationTitle(profile == nil ? tr("新建档案") : tr("编辑档案")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button(tr("保存"), action: save) }
            }
        }.onAppear { name = profile?.name ?? ""; title = profile?.title ?? ""; server = profile?.server ?? "jp"; avatar = profile.flatMap { UIImage(data: Data($0.avatar.map { UInt8(bitPattern: $0) })) } }
        .task(id: photo) {
            guard let photo else { return }
            do {
                if let data = try await photo.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    avatar = image; avatarChanged = true
                }
            } catch { if !Task.isCancelled { self.error = error.localizedDescription } }
        }
    }
    private func save() {
        do {
            let id = try personal.bridge.saveProfile(id: profile?.id, name: name, server: server, title: title)
            if avatarChanged, let avatar {
                let renderer = ImageRenderer(content: Image(uiImage: avatar).resizable().scaledToFill().frame(width: 512, height: 512).clipped())
                if let bytes = renderer.uiImage?.jpegData(compressionQuality: 0.85) { try personal.bridge.setAvatar(profileId: id, base64: bytes.base64EncodedString()) }
            }
            personal.reload(catalog: catalog); dismiss()
        }
        catch { self.error = error.localizedDescription }
    }
}
