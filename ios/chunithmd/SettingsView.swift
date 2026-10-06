import SwiftUI

struct SettingsView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Bindable var account: RhythmetaAccountStore
    @AppStorage("appearance") private var appearance = "system"
    @State private var resources = false
    var body: some View {
        List {
            Section {
                NavigationLink { ProfilesView() } label: {
                    SettingsRowLabel(title: tr("玩家档案"), icon: "person.2.fill", color: .purple)
                }
            } header: { Text(tr("用户管理")) } footer: { Text(tr("为不同玩家或服务器分别保存成绩。")) }
            Section {
                Button { resources = true } label: {
                    HStack {
                        SettingsRowLabel(title: tr("静态数据更新"), icon: "arrow.down.circle.fill", color: .blue)
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
                    }
                }.buttonStyle(.plain)
                NavigationLink { RhythmetaAccountView(store: account) } label: {
                    SettingsRowLabel(title: tr("Rhythmeta 账号与云备份"), icon: "cloud.fill", color: .indigo)
                }
            } header: { Text(tr("数据与云端")) } footer: { Text(tr("登录后可手动备份和恢复个人数据。")) }
            Section {
                NavigationLink { ScoreImportView(provider: "fish") } label: {
                    SettingsRowLabel(title: tr("从水鱼查分器导入"), icon: "fish.fill", color: .blue)
                }
                NavigationLink { ScoreImportView(provider: "lxns") } label: {
                    SettingsRowLabel(title: tr("从落雪咖啡屋导入"), icon: "snowflake", color: .cyan)
                }
                if personal.snapshot.activeProfile?.server == "jp" {
                    NavigationLink { ScoreImportView(provider: "otogame") } label: {
                        SettingsRowLabel(title: tr("从 Otogame 导入"), icon: "clock.arrow.trianglehead.counterclockwise.rotate.90", color: .orange)
                    }
                }
            } header: { Text(tr("成绩导入")) } footer: { Text(tr("导入至当前档案，已有成绩会保留。")) }
            Section(tr("外观")) {
                Picker(selection: $appearance) {
                    Text(tr("跟随系统")).tag("system"); Text(tr("浅色")).tag("light"); Text(tr("深色")).tag("dark")
                } label: { SettingsRowLabel(title: tr("主题"), icon: "moon.fill", color: .indigo) }
            }
            Section(tr("关于")) {
                HStack {
                    SettingsRowLabel(title: tr("版本"), icon: "info.circle.fill", color: .gray)
                    Spacer()
                    Text(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0").foregroundStyle(.secondary)
                }
                if let url = URL(string: "https://github.com/rhythmeta/chunithmd") {
                    Link(destination: url) {
                        SettingsRowLabel(title: tr("源代码"), icon: "chevron.left.forwardslash.chevron.right", color: .gray)
                    }
                }
            }
        }.navigationTitle(tr("设置"))
            .sheet(isPresented: $resources) { StaticResourcesView(store: catalog) }
    }
}
