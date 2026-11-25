import SwiftUI
import Shared

struct CommunityCandidateRow: View {
    let candidate: CommunityViewState.Candidate
    let updatingID: String?
    let canVote: Bool
    let isAdmin: Bool
    let moderate: (CommunityAliasModeration) -> Void
    let vote: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Text(candidate.aliasText).font(.system(size: 16, weight: .semibold))
                    .accessibilityIdentifier("community-alias-" + candidate.id)
                Spacer()
                Text(tr("截止 {0}", deadline)).font(.system(size: 11, weight: .medium)).foregroundStyle(.secondary)
            }
            HStack(spacing: 10) {
                CommunityVoteButton(support: true, count: candidate.supportCount, selected: candidate.myVote == 1,
                                    inFlight: updatingID == candidate.id, alias: candidate.aliasText,
                                    moderate: isAdmin ? { moderate(.approved) } : nil) { vote(true) }
                    .accessibilityIdentifier("community-support-" + candidate.id)
                CommunityVoteButton(support: false, count: candidate.opposeCount, selected: candidate.myVote == -1,
                                    inFlight: updatingID == candidate.id, alias: candidate.aliasText,
                                    moderate: isAdmin ? { moderate(.rejected) } : nil) { vote(false) }
                    .accessibilityIdentifier("community-oppose-" + candidate.id)
                Spacer(minLength: 0)
            }
            .disabled(!canVote || updatingID != nil)
        }
        .padding(.top, 6)
        .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 6, trailing: 16))
    }

    private var deadline: String {
        guard let raw = candidate.voteCloseAt,
              let date = (try? Date(raw, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true))) ?? (try? Date(raw, strategy: .iso8601)) else { return "--" }
        return date.formatted(date: .abbreviated, time: .shortened)
    }
}
