import Shared
import SwiftUI

struct ContentView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var store = CatalogStore()
    @State private var showingFilters = false
    @State private var showingResources = false

    var body: some View {
        NavigationStack {
            Group {
                if store.bundleJson.isEmpty {
                    firstInstall
                } else if store.songs.isEmpty {
                    ContentUnavailableView("没有符合条件的歌曲", systemImage: "music.note.list")
                } else {
                    List(store.songs) { song in
                        SongRow(
                            song: song,
                            jacketURL: store.jacketURL(for: song.imageName),
                            dark: colorScheme == .dark,
                        )
                            .listRowSeparator(.visible)
                    }
                    .listStyle(.plain)
                    .contentMargins(.vertical, 4)
                }
            }
            .navigationTitle("CHUNITHM 曲目")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $store.search, prompt: "曲名、艺术家、别名或 ID")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if !store.bundleJson.isEmpty {
                        Menu {
                            ForEach(sortOptions, id: \.key) { option in
                                Button {
                                    store.sort = option.key
                                } label: {
                                    if store.sort == option.key { Label(option.title, systemImage: "checkmark") }
                                    else { Text(option.title) }
                                }
                            }
                            Divider()
                            Button(store.ascending ? "升序" : "降序", systemImage: store.ascending ? "arrow.up" : "arrow.down") {
                                store.ascending.toggle()
                            }
                        } label: {
                            Label("排序", systemImage: "arrow.up.arrow.down")
                        }
                        .accessibilityLabel("排序方式")

                        Button {
                            showingFilters = true
                        } label: {
                            Label("筛选", systemImage: "line.3.horizontal.decrease")
                        }
                        .accessibilityLabel("筛选歌曲")
                    }
                    Button {
                        showingResources = true
                    } label: {
                        Label("资源", systemImage: "arrow.down.circle")
                    }
                    .accessibilityLabel("静态资源")
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if !store.bundleJson.isEmpty {
                    HStack {
                        Text("\(store.songs.count) 首歌曲")
                        Spacer()
                        Text(store.syncMessage)
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(.bar)
                }
            }
        }
        .task { store.start() }
        .sheet(isPresented: $showingFilters) {
            CatalogFilterSheet(store: store)
        }
        .sheet(isPresented: $showingResources) {
            StaticResourcesView(store: store)
        }
    }

    private var firstInstall: some View {
        VStack(spacing: 16) {
            Image(systemName: "music.note.list")
                .font(.system(size: 38, weight: .medium))
                .foregroundStyle(.tint)
                .accessibilityHidden(true)
            Text("CHUNITHM")
                .font(.title2.weight(.semibold))
            Text(store.errorMessage ?? store.syncMessage)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if store.isSyncing {
                if let progress = store.syncProgress, progress.stage == "Downloading" {
                    VStack(spacing: 8) {
                        ProgressView(value: progress.progress ?? 0)
                            .progressViewStyle(.linear)
                        Text(downloadProgressText(progress))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: 300)
                } else {
                    ProgressView()
                        .controlSize(.regular)
                }
            } else {
                Button("重试", systemImage: "arrow.clockwise") { store.refresh() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var sortOptions: [SortOption] {
        [("default", "默认顺序"), ("title", "标题"), ("versionDate", "版本和发行日期"), ("difficulty", "最高定数")]
            .map { SortOption(key: $0.0, title: $0.1) }
    }
}

private struct SortOption: Identifiable {
    let key: String
    let title: String
    var id: String { key }
}

private struct SongRow: View {
    let song: CatalogSongViewData
    let jacketURL: URL?
    let dark: Bool

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: jacketURL) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    Rectangle()
                        .fill(.quaternary)
                        .overlay {
                            Image(systemName: "music.note")
                                .foregroundStyle(.secondary)
                        }
                }
            }
            .frame(width: 68, height: 68)
            .clipShape(RoundedRectangle(cornerRadius: 7))
            .accessibilityLabel("\(song.title) 曲绘")

            VStack(alignment: .leading, spacing: 5) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(song.title)
                        .font(.body.weight(.semibold))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if let version = song.version {
                        VersionBadge(version: version, dark: dark)
                    }
                }
                Text(song.artist)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                HStack(spacing: 7) {
                    ForEach(difficultyNames, id: \.self) { difficulty in
                        let available = song.sheets.contains { $0.difficulty.caseInsensitiveCompare(difficulty) == .orderedSame && $0.regions?["jp"] == true }
                        Circle()
                            .fill(available ? difficultyColor(difficulty) : Color.secondary.opacity(0.2))
                            .frame(width: 7, height: 7)
                            .overlay(Circle().strokeBorder(available ? .clear : Color.secondary.opacity(0.25), lineWidth: 0.6))
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("谱面可用性：" + difficultyNames.filter { name in song.sheets.contains { $0.difficulty.caseInsensitiveCompare(name) == .orderedSame && $0.regions?["jp"] == true } }.joined(separator: "、"))
            }
        }
        .padding(.vertical, 5)
        .contentShape(Rectangle())
    }

}

private struct VersionBadge: View {
    let version: String
    let dark: Bool

    var body: some View {
        let palette = VersionPalette.shared.forVersion(version: version, dark: dark)
        Text(version.replacingOccurrences(of: " PLUS", with: " +"))
            .font(.caption2.weight(.bold))
            .lineLimit(1)
            .padding(.horizontal, 7)
            .padding(.vertical, 4)
            .foregroundStyle(argbColor(dark ? palette.darkForeground : palette.lightForeground))
            .background(argbColor(dark ? palette.darkBackground : palette.lightBackground), in: RoundedRectangle(cornerRadius: 4))
    }
}

private struct CatalogFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Bindable var store: CatalogStore

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("仅显示 JP 可玩", isOn: $store.playableOnly)
                }
                Section("分类") { choices(store.categories, selection: $store.selectedCategories) }
                Section("版本") { choices(store.versions, selection: $store.selectedVersions) }
                Section("难度") { choices(store.difficulties, selection: $store.selectedDifficulties) }
                Section("谱面类型") { choices(store.types, selection: $store.selectedTypes) }
                Section {
                    Button("清除筛选", role: .destructive) {
                        store.selectedCategories = []
                        store.selectedVersions = []
                        store.selectedDifficulties = []
                        store.selectedTypes = []
                        store.playableOnly = false
                    }
                }
            }
            .navigationTitle("筛选歌曲")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } }
            }
        }
    }

    private func choices(_ options: [String], selection: Binding<Set<String>>) -> some View {
        ForEach(options, id: \.self) { option in
            Button {
                if selection.wrappedValue.contains(option) { selection.wrappedValue.remove(option) }
                else { selection.wrappedValue.insert(option) }
            } label: {
                HStack {
                    Text(option).foregroundStyle(.primary)
                    Spacer()
                    if selection.wrappedValue.contains(option) { Image(systemName: "checkmark").foregroundStyle(.tint) }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }
}

private struct StaticResourcesView: View {
    @Environment(\.dismiss) private var dismiss
    @Bindable var store: CatalogStore

    var body: some View {
        NavigationStack {
            Form {
                Section("当前 bundle") {
                    LabeledContent("版本", value: store.manifest?.version ?? "未安装")
                    LabeledContent("SHA-256") {
                        Text(store.manifest?.sha256 ?? "-")
                            .font(.caption.monospaced())
                            .textSelection(.enabled)
                            .multilineTextAlignment(.trailing)
                    }
                    LabeledContent("更新时间", value: store.manifest?.createdAt ?? "-")
                }
                Section("同步") {
                    LabeledContent("状态", value: store.syncMessage)
                    if let error = store.errorMessage {
                        Text(error).foregroundStyle(.red)
                    }
                    if let progress = store.syncProgress, progress.stage == "Downloading" {
                        ProgressView(value: progress.progress ?? 0)
                            .progressViewStyle(.linear)
                        Text(downloadProgressText(progress))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else if store.isSyncing {
                        ProgressView()
                    }
                    Button("检查更新", systemImage: "arrow.clockwise") { store.checkForUpdate() }
                        .disabled(store.isSyncing)
                    Button(store.updateAvailable ? "下载更新" : (store.manifest == nil ? "下载资源" : "重新安装当前资源"), systemImage: "arrow.down.circle") { store.refresh() }
                        .disabled(store.isSyncing)
                }
            }
            .navigationTitle("静态资源")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }
}

private let difficultyNames = ["basic", "advanced", "expert", "master", "ultima", "world's end"]

private func downloadProgressText(_ progress: CatalogSyncProgressViewData) -> String {
    let bytes = progress.totalBytes.map {
        "\(formatByteCount(progress.downloadedBytes)) / \(formatByteCount($0))"
    } ?? formatByteCount(progress.downloadedBytes)
    let speed = progress.bytesPerSecond > 0
        ? " · \(formatByteCount(progress.bytesPerSecond))/s"
        : ""
    let items = progress.totalItems > 0
        ? " · \(progress.completedItems)/\(progress.totalItems)"
        : ""
    return "\(bytes)\(speed)\(items)"
}

private func formatByteCount(_ bytes: Int64) -> String {
    let units = ["B", "KB", "MB", "GB"]
    var value = Double(max(bytes, 0))
    var unit = 0
    while value >= 1024 && unit < units.count - 1 {
        value /= 1024
        unit += 1
    }
    return unit == 0 ? "\(Int(value)) \(units[unit])" : String(format: "%.1f %@", value, units[unit])
}

private func difficultyColor(_ difficulty: String) -> Color {
    switch difficulty {
    case "basic": Color(red: 0.31, green: 0.68, blue: 0.24)
    case "advanced": Color(red: 0.83, green: 0.67, blue: 0.12)
    case "expert": Color(red: 0.83, green: 0.20, blue: 0.20)
    case "master": Color(red: 0.54, green: 0.27, blue: 0.73)
    case "ultima": Color(red: 0.15, green: 0.15, blue: 0.16)
    default: Color(red: 0.17, green: 0.57, blue: 0.69)
    }
}

private func argbColor(_ value: Int64) -> Color {
    let bits = UInt64(bitPattern: value)
    return Color(
        .sRGB,
        red: Double((bits >> 16) & 0xff) / 255,
        green: Double((bits >> 8) & 0xff) / 255,
        blue: Double(bits & 0xff) / 255,
        opacity: Double((bits >> 24) & 0xff) / 255
    )
}
