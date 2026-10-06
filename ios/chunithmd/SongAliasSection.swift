import Shared
import SwiftUI

struct SongAliasSection: View {
    let songID: String
    @Environment(RhythmetaAccountStore.self) private var account
    @State private var draft = ""
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(tr("社区别名"), systemImage: "person.3.sequence.fill").font(.system(size: 14, weight: .semibold))
                Spacer()
                NavigationLink { CommunityAliasView(showsSongLinks: false) } label: {
                    Text(tr("公示投票")).font(.system(size: 12, weight: .semibold))
                }.buttonStyle(.plain)
            }
            if account.state.user != nil {
                if let used = account.community.dailyUsed {
                    let quota = Int(CommunityAliasModelsKt.CommunityAliasDailyQuota)
                    VStack(alignment: .leading, spacing: 8) {
                        Text(tr("今日已投稿：{0}/{1}", used, quota)).font(.system(size: 11, weight: .semibold)).foregroundStyle(.secondary)
                        ProgressView(value: Double(used), total: Double(quota))
                            .tint(Color(hue: 0.33 * (1 - min(1, Double(used) / Double(quota))), saturation: 0.82, brightness: 0.92))
                    }
                }
                HStack {
                    TextField(tr("提交新的歌曲别名"), text: $draft)
                        .font(.system(size: 13)).textInputAutocapitalization(.never).autocorrectionDisabled()
                        .padding(.horizontal, 12).padding(.vertical, 9)
                        .background(.secondary.opacity(0.08), in: .rect(cornerRadius: 10))
                    Button(tr("提交")) { account.bridge.submitAlias(id: songID, text: draft) }
                        .font(.system(size: 12, weight: .bold)).buttonStyle(.borderedProminent)
                        .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || account.community.songs[songID]?.submitting == true)
                }
                if let message = account.community.songs[songID]?.message { Text(message).font(.caption).foregroundStyle(.secondary) }
                if let error = account.community.songs[songID]?.error { Text(error).font(.caption).foregroundStyle(.red) }
            } else { Text(tr("登录 Rhythmeta 后可投稿别名。")).font(.system(size: 12)).foregroundStyle(.secondary) }
            if let candidates = account.community.songs[songID]?.candidates, !candidates.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text(tr("我的投稿")).font(.system(size: 12, weight: .semibold)).foregroundStyle(.secondary)
                    ForEach(candidates.prefix(4)) { candidate in
                        HStack {
                            Text(candidate.aliasText).font(.system(size: 12, weight: .medium))
                            Spacer()
                            Text(candidate.status == "approved" ? tr("已通过") : candidate.status == "voting" ? tr("投票中") : candidate.status == "rejected" ? tr("已拒绝") : tr("仅自己可见"))
                                .font(.system(size: 10, weight: .bold)).foregroundStyle(.secondary)
                        }.padding(.vertical, 2)
                    }
                }
            }
        }.frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16).padding(.vertical, 14)
            .background(.ultraThinMaterial, in: .rect(cornerRadius: 14))
            .overlay { RoundedRectangle(cornerRadius: 14).strokeBorder(.primary.opacity(0.06)) }
            .task { account.bridge.refreshSongAliases(id: songID) }
    }
}
