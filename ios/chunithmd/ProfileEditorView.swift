import Shared
import SwiftUI

struct ProfileEditorView: View {
    let profile: PersonalSnapshot.Profile?
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var name: String
    @State private var title: String
    @State private var server: String
    @State private var avatarData: Data?
    @State private var avatarChanged = false
    @State private var loadingPhoto = false
    @State private var saving = false
    @State private var error: String?

    init(profile: PersonalSnapshot.Profile?) {
        self.profile = profile
        _name = State(initialValue: profile?.name ?? "")
        _title = State(initialValue: profile?.title ?? "")
        _server = State(initialValue: profile?.server ?? "jp")
        let bytes = profile?.avatar ?? []
        _avatarData = State(initialValue: bytes.isEmpty ? nil : Data(bytes.map { UInt8(bitPattern: $0) }))
    }

    private var canSave: Bool {
        ProfileDraft(name: name, server: ProfileServer.companion.fromWire(value: server), title: title).isValid()
            && !loadingPhoto && !saving
    }

    var body: some View {
        NavigationStack {
            Form {
                ProfileAvatarEditorSection(avatarData: $avatarData, loading: $loadingPhoto, error: $error)
                Section(tr("基本信息")) {
                    TextField(tr("档案名称"), text: $name).accessibilityIdentifier("profile-name")
                    TextField(tr("称号"), text: $title).accessibilityIdentifier("profile-title")
                    Picker(tr("服务器"), selection: $server) {
                        Text(tr("日本")).tag("jp")
                        Text(tr("国际")).tag("intl")
                        Text(tr("中国")).tag("cn")
                    }
                }
            }
            .navigationTitle(profile == nil ? tr("新建档案") : tr("编辑档案"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("取消"), systemImage: "xmark") { dismiss() }
                        .labelStyle(.iconOnly).tint(.primary).disabled(saving)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("保存"), systemImage: "checkmark", action: save)
                        .labelStyle(.iconOnly).tint(.primary).disabled(!canSave)
                }
            }
            .alert(tr("操作失败"), isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button(tr("确定"), role: .cancel) {}
            } message: { Text(error ?? "") }
        }
        .interactiveDismissDisabled(saving)
        .onChange(of: avatarData) { avatarChanged = true }
    }

    private func save() {
        guard canSave else { return }
        saving = true
        defer { saving = false }
        do {
            let id = try personal.bridge.saveProfile(id: profile?.id, name: name, server: server, title: title)
            if avatarChanged {
                try personal.bridge.setAvatar(profileId: id, base64: avatarData?.base64EncodedString() ?? "")
            }
            personal.reload(catalog: catalog)
            dismiss()
        } catch { self.error = error.localizedDescription }
    }
}
