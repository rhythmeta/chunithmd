import Shared
import SwiftUI

struct CommunityAliasView: View {
    var showsSongLinks = true
    @Environment(RhythmetaAccountStore.self) private var account
    @Environment(CatalogStore.self) private var catalog
    var body: some View {
        List {
            if account.state.user == nil {
                Section { NavigationLink { RhythmetaAccountView(store: account) } label: { Label(tr("登录后参与社区投票"), systemImage: "person.crop.circle") } }
            }
            if let error = account.community.boardError ?? account.community.syncError { Text(error).foregroundStyle(.red) }
            ForEach(account.community.board) { candidate in
                VStack(alignment: .leading, spacing: 12) {
                    if let song = catalog.allSongs.first(where: { $0.id == candidate.songIdentifier }) {
                        if showsSongLinks { SongRow(song: song) }
                        else { Text(song.title).font(.subheadline).foregroundStyle(.secondary) }
                    }
                    Text(candidate.aliasText).font(.title3.bold())
                    HStack {
                        Button { account.bridge.voteCommunity(id: candidate.id, vote: 1) } label: { Label("\(candidate.supportCount)", systemImage: candidate.myVote == 1 ? "hand.thumbsup.fill" : "hand.thumbsup") }
                        Spacer()
                        Button { account.bridge.voteCommunity(id: candidate.id, vote: -1) } label: { Label("\(candidate.opposeCount)", systemImage: candidate.myVote == -1 ? "hand.thumbsdown.fill" : "hand.thumbsdown") }
                    }.buttonStyle(.bordered).disabled(account.community.accountId == nil || account.community.votingId != nil)
                }.padding(.vertical, 8)
            }
            if account.community.boardLoading { ProgressView() }
            else if account.community.boardHasMore { Button(tr("加载更多")) { account.bridge.moreCommunity() } }
            else if account.community.board.isEmpty { ContentUnavailableView(tr("暂无待投票别名"), systemImage: "bubble.left.and.bubble.right") }
        }.navigationTitle(tr("社区别名"))
            .task { account.bridge.refreshCommunity() }
            .toolbar { Button(tr("刷新"), systemImage: "arrow.clockwise") { account.bridge.refreshCommunity() } }
    }
}
