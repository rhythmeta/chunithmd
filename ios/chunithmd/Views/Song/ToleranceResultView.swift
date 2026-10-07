import SwiftUI

struct ToleranceResultView: View {
    let title: String
    let value: Int
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(title).font(.system(size: 8, weight: .black)).foregroundStyle(color.opacity(0.8))
            Text(value.formatted()).font(.system(size: 16, weight: .bold, design: .monospaced))
            Text(tr("上限")).font(.system(size: 8)).foregroundStyle(.secondary)
        }.frame(maxWidth: .infinity).padding(.vertical, 10)
            .background(color.opacity(0.08), in: .rect(cornerRadius: 12))
            .overlay { RoundedRectangle(cornerRadius: 12).strokeBorder(color.opacity(0.15)) }
    }
}
