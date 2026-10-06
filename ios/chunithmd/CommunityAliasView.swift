import Shared
import SwiftUI

struct CommunityAliasView: View {
    var showsSongLinks = true
    @Environment(RhythmetaAccountStore.self) private var account
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showsAccount = false
    @State private var tip: String?
    @State private var tipEvent = UUID()

    var body: some View {
        let state = account.community
        let songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let groups = Dictionary(grouping: state.board, by: \.songIdentifier)
        let songIDs = groups.keys.sorted {
            (songs[$0]?.title ?? $0).localizedStandardCompare(songs[$1]?.title ?? $1) == .orderedAscending
        }
        List {
            if state.accountId == nil {
                Section {
                    CommunityMessageCard(title: tr("需要登录"), message: tr("登录后可以投稿和参与社区别名投票。")) {
                        Button { showsAccount = true } label: {
                            Text(tr("登录 Rhythmeta")).font(.system(size: 14, weight: .semibold)).frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent).accessibilityIdentifier("community-login")
                    }
                }
            }
            if let error = state.boardError {
                Section {
                    CommunityMessageCard(title: tr("加载或投票失败"), message: error) {
                        Button(tr("重试")) { account.bridge.refreshCommunity() }.buttonStyle(.bordered)
                    }
                }
            }
            if let error = state.syncError {
                Section {
                    CommunityMessageCard(title: tr("别名同步失败"), message: tr("{0}\n已有的本地别名仍可使用。", error)) {
                        Button(tr("重新同步")) { account.bridge.syncCommunity() }.buttonStyle(.bordered)
                    }
                }
            }
            if state.boardLoading {
                Section {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text(tr("正在加载…")).foregroundStyle(.secondary)
                    }
                }
            } else if state.board.isEmpty && state.boardError == nil {
                Section {
                    CommunityMessageCard(title: tr("当前没有投票中的候选别名"), message: tr("新的候选别名会在公示期显示于此。")) { EmptyView() }
                }
            }
            ForEach(songIDs, id: \.self) { songID in
                let candidates = groups[songID] ?? []
                Section {
                    ForEach(candidates.sorted { ($0.voteCloseAt ?? "~") < ($1.voteCloseAt ?? "~") }) { candidate in
                        CommunityCandidateRow(candidate: candidate, votingID: state.votingId,
                                              canVote: state.accountId != nil && !state.boardLoading) { support in
                            account.bridge.voteCommunity(id: candidate.id, vote: support ? 1 : -1)
                        }
                    }
                } header: {
                    CommunitySongHeader(song: songs[songID], songID: songID, count: candidates.count, showsSongLinks: showsSongLinks)
                }
            }
            if !state.boardLoading && state.boardHasMore && !state.board.isEmpty {
                Section {
                    Button(tr("加载更多")) { account.bridge.moreCommunity() }
                        .frame(maxWidth: .infinity).accessibilityIdentifier("community-more")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("社区别名"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $showsAccount) { RhythmetaAccountView(store: account) }
        .refreshable { await reload() }
        .task(id: state.accountId) { await reload() }
        .onChange(of: state.votingId) { old, new in
            if old != nil, new == nil, state.accountId != nil, state.boardError == nil { showTip(tr("投票已更新")) }
        }
        .overlay(alignment: .bottom) {
            if let tip {
                Text(tip).font(.system(size: 13, weight: .medium)).foregroundStyle(.white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(.black.opacity(0.82), in: .capsule).padding(.bottom, 20)
                    .transition(reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .bottom)))
                    .allowsHitTesting(false).accessibilityIdentifier("community-tip")
            }
        }
        .task(id: tipEvent) {
            guard tip != nil else { return }
            do { try await Task.sleep(for: .seconds(1.6)) } catch { return }
            withAnimation(reduceMotion ? nil : .default) { tip = nil }
        }
    }

    private func reload() async {
        do { try await account.bridge.reloadCommunity() }
        catch { if !Task.isCancelled { showTip(error.localizedDescription) } }
    }

    private func showTip(_ message: String) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.85)) { tip = message }
        tipEvent = UUID()
    }
}
