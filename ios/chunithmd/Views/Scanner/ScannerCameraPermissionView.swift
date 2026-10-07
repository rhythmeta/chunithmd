import SwiftUI

struct ScannerCameraPermissionView: View {
    let request: () -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.fill").font(.largeTitle)
            Text(tr("允许访问相机以实时识别成绩")).multilineTextAlignment(.center)
            Button(tr("允许访问相机"), action: request).buttonStyle(.borderedProminent)
            Button(tr("打开设置")) {
                if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
            }
        }
        .foregroundStyle(.white).padding(24)
        .background(.black.opacity(0.7), in: .rect(cornerRadius: 24)).padding(24)
    }
}
