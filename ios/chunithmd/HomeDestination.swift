import SwiftUI

enum HomeDestination: String, CaseIterable, Identifiable {
    case best, random, recommendation, scores, constants, plate, community, collections
    var id: String { rawValue }
    var title: String {
        switch self {
        case .best: "Best 50"
        case .random: tr("随机选曲")
        case .recommendation: tr("推分推荐")
        case .scores: tr("成绩查询")
        case .constants: tr("定数表")
        case .plate: tr("牌子进度")
        case .community: tr("社区别名")
        case .collections: tr("收藏夹")
        }
    }
    var subtitle: String {
        switch self {
        case .best: tr("B30 + N20 · 你的最佳表现")
        case .random: tr("老虎机式随机抽曲")
        case .recommendation: tr("寻找下一个 Rating 目标")
        case .scores: tr("浏览全部成绩")
        case .constants: tr("生成并分享定数表图片")
        case .plate: tr("查看各版本牌子获取进度")
        case .community: tr("提交、投票与发现歌曲别名")
        case .collections: tr("整理与分享练习曲目")
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
