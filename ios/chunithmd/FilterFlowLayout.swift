import SwiftUI

struct FilterFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = computeRows(subviews: subviews, proposal: proposal)
        if rows.isEmpty { return .zero }

        let totalHeight = rows.reduce(0) { $0 + $1.height } + CGFloat(rows.count - 1) * spacing
        let maxWidth = rows.reduce(0) { max($0, $1.width) }

        return CGSize(width: maxWidth, height: totalHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let rows = computeRows(subviews: subviews, proposal: proposal)
        var rowOriginY = bounds.minY

        for row in rows {
            var itemOriginX = bounds.minX
            for index in row.range {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(
                    at: CGPoint(x: itemOriginX, y: rowOriginY),
                    proposal: ProposedViewSize(size)
                )
                itemOriginX += size.width + spacing
            }
            rowOriginY += row.height + spacing
        }
    }

    private struct Row {
        var range: Range<Int>
        var width: CGFloat
        var height: CGFloat
    }

    private func computeRows(subviews: Subviews, proposal: ProposedViewSize) -> [Row] {
        var rows: [Row] = []
        let maxWidth = proposal.width ?? .infinity

        var currentX: CGFloat = 0
        var currentRowStart = 0
        var currentRowHeight: CGFloat = 0

        for (index, subview) in subviews.enumerated() {
            let size = subview.sizeThatFits(.unspecified)

            if currentX + size.width > maxWidth && index > currentRowStart {
                rows.append(Row(range: currentRowStart..<index, width: currentX - spacing, height: currentRowHeight))
                currentX = 0
                currentRowStart = index
                currentRowHeight = 0
            }

            currentX += size.width + spacing
            currentRowHeight = max(currentRowHeight, size.height)
        }

        if currentRowStart < subviews.count {
            rows.append(
                Row(range: currentRowStart..<subviews.count, width: currentX - spacing, height: currentRowHeight))
        }

        return rows
    }
}
