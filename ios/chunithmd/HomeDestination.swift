import SwiftUI

enum HomeDestination: String, CaseIterable, Identifiable {
    case best, random, recommendation, scores, constants, plate, community, collections
    var id: String { rawValue }
    var title: String {
        switch self {
        case .best: "Best 50"
        case .random: tr("随机歌曲")
        case .recommendation: tr("吃分推荐")
        case .scores: tr("成绩查询")
        case .constants: tr("定数表")
        case .plate: tr("牌子进度")
        case .community: tr("社区别名")
        case .collections: tr("收藏夹")
        }
    }
    var subtitle: String {
        switch self {
        case .best: tr("基于 B{0} + N{1} 计算的玩家 Rating", 30, 20)
        case .random: tr("老虎机式随机抽曲")
        case .recommendation: tr("定数拟合分析")
        case .scores: tr("查询歌曲成绩")
        case .constants: tr("生成并分享定数表图片")
        case .plate: tr("查看各版本牌子达成情况")
        case .community: tr("提交、投票与发现歌曲别名")
        case .collections: tr("整理喜爱的歌曲谱面")
        }
    }
    var icon: String {
        switch self {
        case .best: "trophy.fill"
        case .random: "dice.fill"
        case .recommendation: "sparkles"
        case .scores: "list.bullet.rectangle.portrait.fill"
        case .constants: "square.and.arrow.up.on.square.fill"
        case .plate: "chart.bar.xaxis"
        case .community: "person.3.sequence.fill"
        case .collections: "rectangle.stack.fill"
        }
    }
    var gradient: [Color] {
        switch self {
        case .best, .recommendation: [.orange, .red]
        case .random: [.purple, .pink]
        case .scores: [.indigo, .purple]
        case .constants: [.pink, .orange]
        case .plate: [.green, .blue]
        case .community: [.teal, .green]
        case .collections: [.orange, .green]
        }
    }
    var color: Color {
        switch self {
        case .best, .recommendation: .orange
        case .random: .purple
        case .scores: .indigo
        case .constants: .pink
        case .plate: .green
        case .community: .teal
        case .collections: .orange
        }
    }
}
