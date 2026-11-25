import Shared
import SwiftUI

struct OverPowerSortMenu: View {
    @Binding var order: OverPowerOrder
    var levels = false

    var body: some View {
        Menu(tr("排序"), systemImage: "arrow.up.arrow.down") {
            let options: [OverPowerSort] = levels ? [.remaining, .rate, .level] : [.remaining, .rate, .difficulty, .name]
            Picker(tr("排序"), selection: selectedField.animation(.easeInOut)) {
                ForEach(options, id: \.self) { field in
                    Text(title(field)).tag(field)
                }
            }
            Divider()
            Button {
                withAnimation(.easeInOut) {
                    order = OverPowerOrder(field: order.field, ascending: !order.ascending)
                }
            } label: {
                Label(order.ascending ? tr("升序") : tr("降序"), systemImage: order.ascending ? "arrow.up" : "arrow.down")
            }
        }.tint(.primary).accessibilityIdentifier("overpower-sort")
    }

    private var selectedField: Binding<OverPowerSort> {
        Binding(get: { order.field }, set: { field in
            if field != order.field {
                order = OverPowerOrder(field: field, ascending: field == .name)
            }
        })
    }

    private func title(_ field: OverPowerSort) -> String {
        switch field {
        case .remaining: tr("剩余 OP")
        case .rate: tr("OP 率")
        case .difficulty: tr("难度")
        case .level: tr("等级")
        default: tr("曲名")
        }
    }
}
