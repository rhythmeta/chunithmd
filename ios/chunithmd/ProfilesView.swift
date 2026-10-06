import Shared
import SwiftUI

struct ProfilesView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var editing: PersonalSnapshot.Profile?
    @State private var adding = false
    @State private var deleting: PersonalSnapshot.Profile?
    var body: some View {
        List {
            ForEach(personal.snapshot.profiles) { profile in
                Button { personal.perform(catalog: catalog) { try personal.bridge.activateProfile(id: profile.id) } } label: {
                    HStack {
                        ProfileAvatarView(data: Data(profile.avatar.map { UInt8(bitPattern: $0) })).frame(width: 48, height: 48)
                        VStack(alignment: .leading, spacing: 5) { Text(profile.name).foregroundStyle(.primary); Text(profile.server.uppercased()).font(.caption).foregroundStyle(.secondary) }
                        Spacer()
                        if profile.active { Image(systemName: "checkmark.circle.fill").foregroundStyle(.orange) }
                    }.padding(.vertical, 6)
                }
                .swipeActions { Button(tr("编辑")) { editing = profile }.tint(.blue); if !profile.active { Button(tr("删除"), role: .destructive) { deleting = profile } } }
                .contextMenu { Button(tr("编辑")) { editing = profile }; if !profile.active { Button(tr("删除"), role: .destructive) { deleting = profile } } }
            }
        }
        .navigationTitle(tr("玩家档案"))
        .toolbar { Button(tr("新建档案"), systemImage: "plus") { adding = true } }
        .sheet(isPresented: $adding) { ProfileEditorView(profile: nil) }
        .sheet(item: $editing) { ProfileEditorView(profile: $0) }
        .confirmationDialog(tr("删除档案及其成绩？"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            if let deleting { Button(tr("删除"), role: .destructive) { personal.perform(catalog: catalog) { try personal.bridge.deleteProfile(id: deleting.id) }; self.deleting = nil } }
        }
    }
}
