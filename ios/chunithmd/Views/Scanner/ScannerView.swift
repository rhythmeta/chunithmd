import SwiftUI

/// Reserved for the scanner, matching the current Android destination.
struct ScannerView: View {
    var body: some View {
        AppTheme.page
            .ignoresSafeArea()
            .navigationTitle(tr("扫描"))
    }
}
