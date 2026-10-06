import PhotosUI
import SwiftUI

struct ProfileAvatarEditorSection: View {
    @Binding var avatarData: Data?
    @Binding var loading: Bool
    @Binding var error: String?
    @State private var photo: PhotosPickerItem?
    @State private var editorImage: UIImage?
    @State private var showingCrop = false

    var body: some View {
        Section {
            HStack {
                Spacer()
                VStack(spacing: 14) {
                    Group {
                        if let avatarData {
                            ProfileAvatarView(data: avatarData)
                        } else {
                            Circle().fill(.secondary.opacity(0.1))
                                .overlay { Image(systemName: "person.fill").font(.system(size: 40)).foregroundStyle(.secondary) }
                        }
                    }
                    .frame(width: 100, height: 100)
                    .overlay { Circle().stroke(.primary.opacity(0.1), lineWidth: 1) }
                    .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
                    .accessibilityHidden(true)
                    if loading { ProgressView().controlSize(.small) }
                    ViewThatFits {
                        HStack(spacing: 8) { actions }
                        VStack(spacing: 8) { actions }
                    }
                }
                Spacer()
            }.listRowBackground(Color.clear).padding(.vertical, 10)
        }
        .sheet(isPresented: $showingCrop, onDismiss: { editorImage = nil }) {
            NavigationStack {
                if let editorImage {
                    AvatarCropEditorView(originalImage: editorImage) { avatarData = $0 }
                }
            }
        }
        .task(id: photo) { await loadPhoto() }
    }

    @ViewBuilder private var actions: some View {
        PhotosPicker(selection: $photo, matching: .images) {
            Text(tr("选择头像")).font(.caption.bold()).foregroundStyle(.primary)
                .padding(.horizontal, 12).padding(.vertical, 8)
                .background(.primary.opacity(0.08), in: .capsule)
        }.buttonStyle(.plain).disabled(loading)
        Button(role: .destructive) { avatarData = nil } label: {
            Text(tr("清除头像")).font(.caption.bold()).foregroundStyle(.red)
                .padding(.horizontal, 12).padding(.vertical, 8)
                .background(.red.opacity(0.12), in: .capsule)
        }.buttonStyle(.plain).disabled(loading || avatarData == nil).opacity(avatarData == nil ? 0.45 : 1)
    }

    private func loadPhoto() async {
        guard let photo else { return }
        loading = true
        defer { loading = false }
        do {
            let data = try await photo.loadTransferable(type: Data.self)
            try Task.checkCancellation()
            guard let data, let image = UIImage(data: data) else {
                error = tr("操作失败，请重试")
                self.photo = nil
                return
            }
            editorImage = image
            showingCrop = true
            self.photo = nil
        } catch {
            if !Task.isCancelled { self.error = error.localizedDescription; self.photo = nil }
        }
    }
}
