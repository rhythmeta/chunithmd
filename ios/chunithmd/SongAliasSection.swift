import Shared
import SwiftUI

struct SongAliasSection: View {
    let songID: String
    @Environment(RhythmetaAccountStore.self) private var account
    @State private var draft = ""
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(tr("社区别名"), systemImage: "person.3.sequence.fill").font(.system(size: 14, weight: .semibold))
            if let aliases = account.community.approvedAliases[songID], !aliases.isEmpty { Text(aliases.joined(separator: "、")).font(.system(size: 12)) }
            if account.state.user != nil {
                HStack {
                    TextField(tr("提交新的歌曲别名"), text: $draft)
                        .font(.system(size: 13)).textInputAutocapitalization(.never).autocorrectionDisabled()
                        .padding(.horizontal, 12).padding(.vertical, 9)
                        .background(.secondary.opacity(0.08), in: .rect(cornerRadius: 10))
                    Button(tr("提交")) { account.bridge.submitAlias(id: songID, text: draft) }
                        .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || account.community.songs[songID]?.submitting == true)
                }
                if let message = account.community.songs[songID]?.message { Text(message).font(.caption).foregroundStyle(.secondary) }
                if let error = account.community.songs[songID]?.error { Text(error).font(.caption).foregroundStyle(.red) }
            } else { Text(tr("登录 Rhythmeta 后可投稿别名。")).font(.system(size: 12)).foregroundStyle(.secondary) }
        }.frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16).padding(.vertical, 14)
            .background(.ultraThinMaterial, in: .rect(cornerRadius: 14))
            .overlay { RoundedRectangle(cornerRadius: 14).stroke(.primary.opacity(0.06)) }
            .task { account.bridge.refreshSongAliases(id: songID) }
    }
}
