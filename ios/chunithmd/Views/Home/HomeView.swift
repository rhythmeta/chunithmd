import SwiftUI

struct HomeView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var editingProfile = false
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HomeProfileCard { editingProfile = true }
                NavigationLink(value: HomeDestination.best) {
                    HStack {
                        Image(systemName: "trophy.fill").font(.system(size: 20)).foregroundStyle(.orange)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(tr("查看 Best 50 成绩表")).font(.system(size: 16, weight: .bold))
                            Text(tr("基于 B{0} + N{1} 计算的玩家 Rating", 30, 20)).font(.system(size: 11)).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.system(size: 14, weight: .bold)).foregroundStyle(.secondary.opacity(0.5))
                    }
                    .padding(.horizontal, 20).padding(.vertical, 14)
                    .background(.orange.opacity(0.1), in: .rect(cornerRadius: 16))
                    .overlay { RoundedRectangle(cornerRadius: 16).stroke(.orange.opacity(0.2)) }
                }.buttonStyle(.plain)
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)], spacing: 16) {
                    ForEach(HomeDestination.allCases.filter { $0 != .best }) { destination in
                        NavigationLink(value: destination) { HomeFeatureCard(destination: destination) }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("home-" + destination.rawValue)
                    }
                }
                if catalog.bundle == nil { CatalogLoadingView().frame(minHeight: 180) }
            }.padding(16)
        }
        .background(AppTheme.page)
        .navigationTitle(tr("主页"))
        .navigationDestination(for: HomeDestination.self) { destination in
            switch destination {
            case .best: BestTableView()
            case .scores: ScoreQueryView()
            case .constants: ConstantTableView()
            case .random: RandomSongView()
            case .recommendation: RecommendationView()
            case .plate: PlateProgressView()
            case .community: CommunityAliasView()
            case .collections: CollectionsView()
            }
        }
        .sheet(isPresented: $editingProfile) { ProfileEditorView(profile: personal.snapshot.activeProfile) }
    }
}
