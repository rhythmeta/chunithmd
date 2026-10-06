import SwiftUI

struct CloudBackupRestoreButton: View {
    let backup: RhythmetaViewState.Backup
    let restore: () -> Void
    @State private var showingConfirmation = false

    var body: some View {
        Button { showingConfirmation = true } label: {
            CloudBackupRow(backup: backup)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("cloud-restore-" + backup.id)
        .confirmationDialog(tr("从云端恢复"), isPresented: $showingConfirmation, titleVisibility: .visible) {
            Button(tr("从云端恢复"), role: .destructive, action: restore)
            Button(tr("取消"), role: .cancel) {}
        } message: {
            Text(tr("这将替换本机该游戏的全部个人数据。恢复前会保存本地回滚副本。"))
        }
    }
}
