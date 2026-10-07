import Foundation

struct PersonalSnapshot: Decodable {
    struct Profile: Decodable, Identifiable {
        let id: String
        let name: String
        let avatar: [Int8]
        let server: String
        let title: String
        let active: Bool
    }
    struct Score: Decodable {
        let profileId: String
        let chartKey: String
        let songId: String
        let score: Int
        let rank: String
        let achievedAt: Int64
        let clear: String
        let fc: String
        let fs: String
    }
    struct Record: Decodable, Identifiable {
        let id: String
        let result: Score
    }
    struct Collection: Decodable, Identifiable {
        let id: String
        let name: String
    }
    struct Item: Decodable, Identifiable {
        let id: String
        let collectionId: String
        let songId: String
        let chartType: String
        let difficulty: String
    }
    var profiles: [Profile] = []
    var scores: [Score] = []
    var playRecords: [Record] = []
    var collections: [Collection] = []
    var collectionItems: [Item] = []
    var favoriteSongIds: [String] = []
    var activeProfile: Profile? { profiles.first { $0.active } }
}
