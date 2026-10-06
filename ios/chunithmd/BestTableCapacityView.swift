import SwiftUI
import Shared

struct BestTableCapacityView: View {
    let preferences: BestTablePreferences
    let total: Int32
    let onCommit: (Int32, Int32) -> Void
    @State private var bestDraft = ""
    @State private var newDraft = ""
    @FocusState private var focusedField: Bool?

    var body: some View {
        HStack(spacing: 20) {
            VStack(spacing: 6) {
                Text(tr("Best 数量")).font(.caption2).foregroundStyle(.secondary)
                TextField("", text: $bestDraft)
                    .focused($focusedField, equals: false)
                    .accessibilityLabel(tr("Best 数量"))
                    .accessibilityIdentifier("best-capacity")
            }
            Image(systemName: "plus").font(.caption.bold()).foregroundStyle(.secondary)
            VStack(spacing: 6) {
                Text(tr("New 数量")).font(.caption2).foregroundStyle(.secondary)
                TextField("", text: $newDraft)
                    .focused($focusedField, equals: true)
                    .accessibilityLabel(tr("New 数量"))
                    .accessibilityIdentifier("new-capacity")
            }
            Divider().frame(height: 30)
            VStack(spacing: 4) {
                Text(tr("总计")).font(.caption2).foregroundStyle(.secondary)
                Text(total.formatted()).font(.system(.body, design: .rounded).bold()).foregroundStyle(.orange)
                    .accessibilityIdentifier("best-total-capacity")
            }.frame(maxWidth: .infinity)
        }
        .textFieldStyle(BestCapacityFieldStyle())
        .padding(.vertical, 4)
        .toolbar {
            if focusedField != nil {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button(tr("完成")) { focusedField = nil }
                }
            }
        }
        .onAppear(perform: synchronize)
        .onChange(of: preferences.bestCount) { synchronize() }
        .onChange(of: preferences.newCount) { synchronize() }
        .onChange(of: focusedField) { old, _ in if old != nil { commit() } }
        .onDisappear { if focusedField != nil { commit() } }
    }

    private func synchronize() {
        if focusedField != false { bestDraft = String(preferences.bestCount) }
        if focusedField != true { newDraft = String(preferences.newCount) }
    }

    private func commit() {
        let normalized = BestTablePreferences(bestCount: Int32(bestDraft) ?? preferences.bestCount,
            newCount: Int32(newDraft) ?? preferences.newCount, selectedVersion: nil).normalized()
        bestDraft = String(normalized.bestCount)
        newDraft = String(normalized.newCount)
        if normalized.bestCount != preferences.bestCount || normalized.newCount != preferences.newCount {
            onCommit(normalized.bestCount, normalized.newCount)
        }
    }
}
