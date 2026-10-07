import SwiftUI
import Shared

struct BestTableVersionPicker: View {
    let versions: [String]
    let serverVersion: String?
    @Binding var selection: String?
    @State private var draft: String?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button { draft = nil } label: {
                        HStack {
                            Text(tr("自动"))
                            Spacer()
                            if draft == nil { Image(systemName: "checkmark") }
                        }.contentShape(.rect)
                    }.buttonStyle(.plain).accessibilityIdentifier("best-version-auto")
                } footer: {
                    if let serverVersion { Text(CatalogVersionFormatter.shared.badge(version: serverVersion)) }
                }
                Section(tr("游戏版本")) {
                    ForEach(versions.reversed(), id: \.self) { version in
                        Button { draft = version } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(CatalogVersionFormatter.shared.badge(version: version))
                                    Text(version).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if draft == version { Image(systemName: "checkmark") }
                            }.contentShape(.rect)
                        }.buttonStyle(.plain).accessibilityIdentifier("best-version-option-" + version)
                    }
                }
            }
            .navigationTitle(tr("游戏版本")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() }.tint(.primary) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(tr("确定")) { selection = draft; dismiss() }.tint(.primary).bold()
                }
            }
        }
        .onAppear { draft = selection }
        .presentationDetents([.medium, .large])
    }
}
