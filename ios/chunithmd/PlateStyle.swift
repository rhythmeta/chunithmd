import Shared
import SwiftUI

func plateColor(_ type: PlateType) -> Color {
    switch type {
    case .tribute: argbColor(0xFFD49A2B)
    case .legend: argbColor(0xFFE45D7B)
    default: argbColor(0xFF39A66B)
    }
}
