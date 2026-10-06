import Shared
import SwiftUI

struct ProfilesView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var editing: PersonalSnapshot.Profile?
    @State private var adding = false
    @State private var deleting: PersonalSnapshot.Profile?
    @State private var serverVersions: [String: String] = [:]

    var body: some View {
        List {
            if personal.snapshot.profiles.isEmpty {
                ContentUnavailableView {
                    Label(tr("还没有用户档案"), systemImage: "person.crop.circle.badge.plus")
                } description: {
                    Text(tr("创建档案以保存你的本地设置"))
                }
            } else {
                ForEach(personal.snapshot.profiles) { profile in
                    Button {
                        if !profile.active {
                            personal.perform(catalog: catalog) { try personal.bridge.activateProfile(id: profile.id) }
                        }
                    } label: {
                        ProfileRow(profile: profile, version: serverVersions[profile.server])
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("profile-row-" + profile.id)
                    .accessibilityValue(profile.active ? tr("当前档案") : "")
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        if !profile.active {
                            Button(tr("删除"), systemImage: "trash", role: .destructive) { deleting = profile }
                        }
                        Button(tr("编辑"), systemImage: "pencil") { editing = profile }.tint(.blue)
                    }
                    .contextMenu {
                        Button(tr("编辑"), systemImage: "pencil") { editing = profile }
                        if !profile.active {
                            Button(tr("删除"), systemImage: "trash", role: .destructive) { deleting = profile }
                        }
                    }
                }
            }
        }
        .navigationTitle(tr("用户档案"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("新建档案"), systemImage: "plus") { adding = true }
                    .labelStyle(.iconOnly).tint(.primary)
            }
        }
        .sheet(isPresented: $adding) { ProfileEditorView(profile: nil) }
        .sheet(item: $editing) { ProfileEditorView(profile: $0) }
        .confirmationDialog(tr("删除档案及其成绩？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible, presenting: deleting) { profile in
            Button(tr("删除"), role: .destructive) {
                deleting = nil
                personal.perform(catalog: catalog) { try personal.bridge.deleteProfile(id: profile.id) }
            }
            Button(tr("取消"), role: .cancel) { deleting = nil }
        } message: { _ in
            Text(tr("删除后本地档案信息将无法恢复。"))
        }
        .task(id: catalog.bundle) {
            guard let bundle = catalog.bundle else { serverVersions = [:]; return }
            var versions: [String: String] = [:]
            for server in ["jp", "intl", "cn"] {
                if let version = bundle.latestPlayableVersion(server: ProfileServer.companion.fromWire(value: server)) {
                    versions[server] = CatalogVersionFormatter.shared.badge(version: version)
                }
            }
            serverVersions = versions
        }
    }
}
