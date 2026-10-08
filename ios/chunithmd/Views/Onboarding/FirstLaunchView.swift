import SwiftUI

struct FirstLaunchView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(tr("欢迎使用")).font(.title2)
                        Text("chunithmd").font(.largeTitle.bold()).foregroundStyle(.tint)
                    }
                    Spacer(minLength: 0)
                    Image("OnboardingIcon").resizable().scaledToFit().frame(width: 88, height: 88)
                        .accessibilityHidden(true)
                }
                Text(tr("记录每一次进步。")).font(.title3).foregroundStyle(.secondary)
                VStack(alignment: .leading, spacing: 28) {
                    OnboardingFeatureRow(icon: "camera.viewfinder", title: tr("快速扫描"),
                        detail: tr("竖持查歌，横持查分。首次扫描时下载识别模型。"))
                    OnboardingFeatureRow(icon: "lock.fill", title: tr("本地离线"),
                        detail: tr("资源下载后，离线也能查看歌曲与记录成绩。"))
                    OnboardingFeatureRow(icon: "person.2.fill", title: tr("多档案管理"),
                        detail: tr("分别管理不同玩家和服务器的成绩，在设置中创建或切换档案。"))
                }
            }
            .frame(maxWidth: 512, alignment: .leading).padding(.horizontal, 24).padding(.vertical, 40)
            .frame(maxWidth: .infinity)
        }
        .scrollIndicators(.hidden)
        .background(AppTheme.page.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) { OnboardingDownloadAction() }
        .accessibilityIdentifier("onboarding")
    }
}
