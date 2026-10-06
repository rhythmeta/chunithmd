import Shared
import SwiftUI

struct ContentView: View {
    enum TabSelection { case home, collections, settings, search }
    @State private var navigation = SongNavigation()
    @State private var catalog = CatalogStore()
    @State private var personal = PersonalStore()
    @State private var account = RhythmetaAccountStore()
    @State private var collectionLink = ""
    @State private var importingCollection = false
    @State private var selection = TabSelection.home
    @FocusState private var searchFocused: Bool
    @AppStorage("appearance") private var appearance = "system"

    var body: some View {
        TabView(selection: $selection) {
            Tab(tr("首页"), systemImage: "house", value: .home) {
                NavigationStack { HomeView() }
            }
            Tab(tr("收藏"), systemImage: "folder", value: .collections) {
                NavigationStack { CollectionsView() }
            }
            Tab(tr("设置"), systemImage: "gearshape", value: .settings) {
                NavigationStack { SettingsView(account: account) }
            }
            Tab(tr("歌曲"), systemImage: "magnifyingglass", value: .search, role: .search) {
                NavigationStack {
                    CatalogView()
                        .searchable(text: $catalog.search, prompt: tr("曲名、艺术家、别名或 ID"))
                        .searchFocused($searchFocused)
                }
            }
        }
        .accessibilityHidden(navigation.song != nil)
        .overlay { if navigation.song != nil { SongDetailOverlay() } }
        .environment(navigation)
        .tint(.blue)
        .environment(catalog)
        .environment(personal)
        .environment(account)
        .preferredColorScheme(appearance == "system" ? nil : appearance == "dark" ? .dark : .light)
        .onChange(of: navigation.song) { _, song in
            if song != nil { searchFocused = false }
        }
        .task { catalog.start(); personal.reload(catalog: catalog) }
        .onChange(of: catalog.bundleJson) { personal.reload(catalog: catalog) }
        .onChange(of: account.community.approvedAliases) { updateAliases() }
        .onChange(of: account.community.personalAliases) { updateAliases() }
        .onChange(of: account.state.busy) { _, busy in if !busy { personal.reload(catalog: catalog) } }
        .onOpenURL { url in
            if url.scheme == "chunithmd", url.host == "auth" { account.bridge.handleCallback(url: url.absoluteString) }
            else if (url.scheme == "chunithmd" && url.host == "collection") || (url.host == "dash.rhythmeta.org" && url.path.hasPrefix("/collection/")) {
                collectionLink = url.absoluteString; importingCollection = true
            }
        }
        .sheet(isPresented: $importingCollection) { CollectionImportView(initialText: collectionLink) }
        .alert(tr("无法完成操作"), isPresented: Binding(get: { personal.error != nil }, set: { if !$0 { personal.error = nil } })) {
            Button(tr("好")) { personal.error = nil }
        } message: { Text(personal.error ?? "") }
    }
    private func updateAliases() {
        catalog.aliases = account.community.approvedAliases.merging(account.community.personalAliases) { $0 + $1 }
    }
}
