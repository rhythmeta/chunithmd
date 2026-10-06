import Shared
import SwiftUI

struct ScoreEntryView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var score: Int?
    @State private var clear = "clear"
    @State private var combo = ""
    @State private var chain = ""
    @State private var error: String?
    var body: some View {
        NavigationStack {
            Form {
                Section { Text(song.title).font(.headline); Text(sheet.difficulty.uppercased()).foregroundStyle(difficultyStyle(sheet.difficulty)) }
                Section(tr("成绩")) {
                    TextField("0–1,010,000", value: $score, format: .number.grouping(.never)).keyboardType(.numberPad)
                    Picker(tr("通关"), selection: $clear) {
                        Text(tr("无")).tag("")
                        ForEach(["failed", "clear", "hard", "brave", "absolute", "catastrophy"], id: \.self) { Text($0.uppercased()).tag($0) }
                    }
                    Picker(tr("连击"), selection: $combo) {
                        Text(tr("无")).tag(""); Text("FC").tag("fullcombo"); Text("AJ").tag("alljustice"); Text("AJC").tag("alljusticecritical")
                    }
                    Picker(tr("Full Chain"), selection: $chain) {
                        Text(tr("无")).tag(""); Text(tr("铂 FC")).tag("fullchain"); Text(tr("金 FC")).tag("fullchain2")
                    }
                }
                if let error { Text(error).foregroundStyle(.red) }
            }
            .navigationTitle(tr("录入成绩")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button(tr("保存"), action: save).disabled(score == nil) }
            }
        }
    }
    private func save() {
        guard let score, let value = Int32(exactly: score) else { error = tr("请输入有效分数"); return }
        do {
            try personal.bridge.saveScore(songId: song.id, type: sheet.type, difficulty: sheet.difficulty, score: value, clear: clear, combo: combo, chain: chain)
            personal.reload(catalog: catalog); dismiss()
        } catch { self.error = error.localizedDescription }
    }
}
