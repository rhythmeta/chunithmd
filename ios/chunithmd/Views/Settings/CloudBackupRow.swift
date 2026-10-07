import SwiftUI

struct CloudBackupRow: View {
    let backup: RhythmetaViewState.Backup

    private var date: Date? {
        (try? Date(backup.committedAt, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true)))
            ?? (try? Date(backup.committedAt, strategy: .iso8601))
    }

    var body: some View {
        HStack {
            Image(systemName: "icloud.and.arrow.down.fill")
                .foregroundStyle(.green).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                if let date {
                    Text(date, format: .dateTime.year().month().day().hour().minute()).foregroundStyle(.primary)
                } else {
                    Text(backup.committedAt).foregroundStyle(.primary)
                }
                Text(backup.deviceName).font(.subheadline).foregroundStyle(.secondary)
                Text(tr("{0} 个档案", backup.profileCount)).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold)).foregroundStyle(.tertiary).accessibilityHidden(true)
        }
        .frame(minHeight: 44)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityHint(tr("从云端恢复"))
    }
}
