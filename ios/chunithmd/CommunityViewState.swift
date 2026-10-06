import Foundation

struct CommunityViewState: Decodable {
    struct Candidate: Decodable, Identifiable {
        let candidateId: String
        let songIdentifier: String
        let aliasText: String
        let status: String
        let supportCount: Int
        let opposeCount: Int
        let myVote: Int?
        var id: String { candidateId }
    }
    struct Song: Decodable {
        let draft: String
        let loading: Bool
        let submitting: Bool
        let candidates: [Candidate]
        let message: String?
        let error: String?
    }
    var accountId: String?
    var approvedAliases: [String: [String]] = [:]
    var personalAliases: [String: [String]] = [:]
    var songs: [String: Song] = [:]
    var board: [Candidate] = []
    var boardLoading = false
    var boardHasMore = true
    var votingId: String?
    var boardError: String?
    var syncError: String?
}
