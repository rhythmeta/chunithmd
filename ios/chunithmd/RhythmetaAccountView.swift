import AuthenticationServices
import Shared
import SwiftUI

struct RhythmetaAccountView: View {
    @Environment(\.webAuthenticationSession) private var webAuthenticationSession
    @Bindable var store: RhythmetaAccountStore
    @State private var authenticating = false
    @State private var notice: String?
    @State private var noticeIsError = false

    var body: some View {
        CloudAccountContent(
            state: store.state,
            authenticate: { mode in Task { await authenticate(mode: mode) } },
            backup: { store.bridge.backup(deviceName: "chunithmd iOS") },
            refresh: { store.bridge.refresh() },
            restore: { store.bridge.restore(id: $0.id) },
            logout: { store.bridge.logout() })
            .disabled(store.state.busy || authenticating || !store.state.ready)
            .navigationTitle(tr("Rhythmeta"))
            .navigationBarTitleDisplayMode(.inline)
            .interactiveDismissDisabled(store.state.busy)
            .navigationBarBackButtonHidden(store.state.busy)
            .overlay(alignment: .bottom) {
                if let notice {
                    CloudAccountNotice(message: notice, isError: noticeIsError)
                        .padding(20)
                }
            }
            .task(id: notice) {
                guard notice != nil else { return }
                do { try await Task.sleep(for: .seconds(4)) } catch { return }
                notice = nil
            }
            .task(id: store.state.ready) {
                if store.state.ready && store.state.user != nil && !store.state.busy {
                    store.bridge.refresh()
                }
            }
            .onChange(of: store.state.message, initial: true) { _, message in
                if let message { showNotice(message, isError: false) }
            }
            .onChange(of: store.state.error, initial: true) { _, error in
                if let error { showNotice(error, isError: true) }
            }
            .onChange(of: store.loginError, initial: true) { _, error in
                if let error { showNotice(error, isError: true) }
            }
    }

    private func showNotice(_ message: String, isError: Bool) {
        noticeIsError = isError
        notice = message
    }

    private func authenticate(mode: String) async {
        guard !authenticating else { return }
        authenticating = true
        store.loginError = nil
        notice = nil
        defer { authenticating = false }
        do {
            guard let url = URL(string: try store.bridge.loginUrl(mode: mode)) else { return }
            let callback = try await webAuthenticationSession.authenticate(
                using: url, callback: .customScheme("chunithmd"), additionalHeaderFields: [:])
            store.bridge.handleCallback(url: callback.absoluteString)
        } catch {
            guard !Task.isCancelled,
                  (error as? ASWebAuthenticationSessionError)?.code != .canceledLogin else { return }
            store.loginError = error.localizedDescription
        }
    }
}
