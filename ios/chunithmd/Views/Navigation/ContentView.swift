import Shared
import SwiftUI

struct ContentView: View {
    enum TabSelection { case home, scan, search, settings }
    @State private var navigation = SongNavigation()
    @Namespace private var songTransitionNamespace
    @State private var catalog = CatalogStore()
    @State private var personal = PersonalStore()
    @State private var account = RhythmetaAccountStore()
    @State private var selection = TabSelection.home
    @State private var homePath: [HomeDestination] = []
    @FocusState private var searchFocused: Bool
    @AppStorage("onboarding.completed") private var onboardingCompleted = false
    @AppStorage("appearance") private var appearance = "system"

    var body: some View {
        @Bindable var navigation = navigation
        Group {
            if !catalog.hasLoadedLocal {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity).background(AppTheme.page)
            } else if OnboardingPolicy.shared.needsOnboarding(completed: onboardingCompleted, hasCatalog: catalog.bundle != nil) {
                FirstLaunchView()
            } else {
                TabView(selection: $selection) {
                    Tab(tr("主页"), systemImage: "house", value: .home) {
                        NavigationStack(path: $homePath) { HomeView() }
                    }
                    Tab(tr("扫描"), systemImage: "camera.viewfinder", value: .scan) {
                        NavigationStack { ScannerView() }
                    }
                    Tab(tr("歌曲"), systemImage: "magnifyingglass", value: .search) {
                        NavigationStack {
                            CatalogView()
                                .searchable(text: $catalog.search, prompt: tr("歌曲、艺术家、别名..."))
                                .searchFocused($searchFocused)
                        }
                    }
                    Tab(tr("设置"), systemImage: "gearshape", value: .settings) {
                        NavigationStack { SettingsView(account: account) }
                    }
                }
            }
        }
        .fullScreenCover(item: $navigation.song) { song in
            NavigationStack {
                SongDetailView(song: song, preferredSheet: navigation.preferredSheet)
            }
            .navigationTransition(.zoom(sourceID: navigation.sourceID, in: songTransitionNamespace))
        }
        .environment(\.songTransitionNamespace, songTransitionNamespace)
        .environment(navigation)
        .tint(.blue)
        .environment(catalog)
        .environment(personal)
        .environment(account)
        .preferredColorScheme(appearance == "system" ? nil : appearance == "dark" ? .dark : .light)
        .onChange(of: navigation.song) { _, song in
            if song != nil { searchFocused = false }
        }
        .task {
            catalog.start(automaticallyDownload: onboardingCompleted)
            if catalog.bundle != nil { onboardingCompleted = true }
            personal.reload(catalog: catalog)
        }
        .onChange(of: catalog.bundleJson) {
            if catalog.bundle != nil { onboardingCompleted = true }
            personal.reload(catalog: catalog)
        }
        .onChange(of: account.community.approvedAliases) { updateAliases() }
        .onChange(of: account.community.personalAliases) { updateAliases() }
        .onChange(of: account.state.busy) { _, busy in if !busy { personal.reload(catalog: catalog) } }
        .onOpenURL { url in
            if url.scheme == "chunithmd", url.host == "auth" { account.bridge.handleCallback(url: url.absoluteString) }
            else if (url.scheme == "chunithmd" && url.host == "collection") || (url.host == "dash.rhythmeta.org" && url.path.hasPrefix("/collection/")) {
                selection = .home
                homePath = [.collections]
                personal.perform(catalog: catalog) {
                    _ = try personal.bridge.importCollection(text: url.absoluteString)
                }
            }
        }
        .alert(tr("无法完成操作"), isPresented: Binding(get: { personal.error != nil }, set: { if !$0 { personal.error = nil } })) {
            Button(tr("好")) { personal.error = nil }
        } message: { Text(personal.error ?? "") }
    }
    private func updateAliases() {
        catalog.aliases = account.community.approvedAliases.merging(account.community.personalAliases) { $0 + $1 }
    }
}
