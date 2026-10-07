import AVFoundation
import PhotosUI
import SwiftUI

struct ScannerView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var store = ScannerStore()
    @State private var models = ScannerModelController()
    @State private var photo: PhotosPickerItem?
    @State private var showPhotos = false
    @State private var entry: ScannerEntrySelection?
    @State private var visible = false
    @State private var cameraAllowed = false
    @State private var cameraFailed = false
    @State private var landscape = 0

    private var ready: Bool { models.state.usable && catalog.bundle != nil && personal.snapshot.activeProfile != nil }
    private var live: Bool { visible && scenePhase == .active && ready && !store.photoMode && !showPhotos && entry == nil }
    private var song: CatalogSongViewData? { catalog.allSongs.first { $0.id == store.result?.match.songId } }
    private var status: String? {
        if let feedback = store.feedback { return feedback }
        if let error = store.error { return error }
        if !models.state.usable { return nil }
        if !ready { return tr("请先加载曲库并选择玩家档案。") }
        if !store.photoMode && cameraFailed { return tr("相机暂不可用，请使用相册识别。") }
        if !store.photoMode && cameraAllowed && landscape == 0 { return tr("请横持手机，将成绩画面对准取景框") }
        return nil
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if store.photoMode {
                if let image = store.preview {
                    Image(uiImage: image).resizable().scaledToFit().ignoresSafeArea().accessibilityLabel(tr("成绩图"))
                }
            } else if cameraAllowed && models.state.usable {
                ScannerCameraPreview(enabled: live, analyzing: live, landscape: landscape,
                    onFrame: recognizeFrame, onError: { cameraFailed = true })
                    .ignoresSafeArea()
            }
            VStack {
                HStack(spacing: 16) {
                    Spacer()
                    if store.photoMode {
                        Button(tr("返回实时扫描"), systemImage: "xmark", action: reset)
                            .scannerOverlayButton()
                    }
                    Button(tr("选择成绩图"), systemImage: "photo.on.rectangle.angled") { showPhotos = true }
                        .scannerOverlayButton().disabled(!ready || store.busy)
                        .accessibilityIdentifier("scanner-photo-library")
                }.padding()
                Spacer()
                if store.busy {
                    ProgressView(tr("正在识别…")).tint(.white).foregroundStyle(.white)
                        .padding().background(.black.opacity(0.65), in: .capsule)
                }
                if let status {
                    Text(status).font(.subheadline.weight(.medium)).foregroundStyle(.white).multilineTextAlignment(.center)
                        .padding(.horizontal, 20).padding(.vertical, 12)
                        .background(.black.opacity(0.65), in: .capsule).padding(.horizontal, 24)
                }
                if let result = store.result, let song {
                    if !store.photoMode {
                        Button { Task { await store.savePhoto() } } label: {
                            ZStack {
                                Circle().stroke(.white, lineWidth: 3).frame(width: 64, height: 64)
                                Circle().fill(store.savingPhoto ? .gray : .white).frame(width: 54, height: 54)
                                if store.savingPhoto { ProgressView().tint(.white) }
                            }
                        }
                        .disabled(store.savingPhoto)
                        .accessibilityLabel(tr("保存成绩照片"))
                        .padding(.top, 12).padding(.bottom, 20)
                    }
                    ScannerResultCardView(result: result, song: song, onTap: openEntry)
                        .padding(.horizontal, 20).padding(.bottom, 24)
                        .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity).combined(with: .scale(scale: 0.9)))
                } else { Spacer() }
            }
            if !models.state.usable {
                ScannerModelDownloadView(models: models)
            } else if !cameraAllowed && !store.photoMode {
                ScannerCameraPermissionView(request: requestCamera)
            }
        }
        .overlay(alignment: .top) {
            if models.state.usable && models.state.stage != "ready" {
                ScannerModelDownloadView(models: models).padding(.top, 52)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .tabBar)
        .animation(reduceMotion ? nil : .snappy, value: store.result?.match.id)
        .photosPicker(isPresented: $showPhotos, selection: $photo, matching: .images)
        .sheet(item: $entry, onDismiss: reset) { selection in
            ScoreEntryView(song: selection.song, sheet: selection.sheet, initialScore: selection.result.score,
                initialClear: selection.result.clear, profileID: selection.profileID, region: selection.region)
        }
        .onAppear(perform: appear)
        .onDisappear(perform: disappear)
        .onReceive(NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)) { _ in updateOrientation() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { cameraAllowed = AVCaptureDevice.authorizationStatus(for: .video) == .authorized }
            else { store.invalidate() }
        }
        .onChange(of: models.state.usable) { _, usable in if usable { requestCamera() } }
        .onChange(of: showPhotos) { _, showing in if showing { store.invalidate() } }
        .onChange(of: personal.snapshot.activeProfile?.id) { entry = nil; reset() }
        .onChange(of: personal.snapshot.activeProfile?.server) { entry = nil; reset() }
        .task(id: photo) { await recognizePhoto() }
        .task(id: store.feedback) {
            guard store.feedback != nil else { return }
            do { try await Task.sleep(for: .seconds(2.5)); store.feedback = nil } catch { }
        }
    }

    private func appear() {
        visible = true
        ScannerOrientationPolicy.setScanning(true)
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        updateOrientation()
        cameraAllowed = AVCaptureDevice.authorizationStatus(for: .video) == .authorized
        models.start()
        if models.state.usable { requestCamera() }
    }
    private func disappear() {
        visible = false; reset()
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
        ScannerOrientationPolicy.setScanning(false)
    }
    private func updateOrientation() {
        switch UIDevice.current.orientation {
        case .landscapeLeft: landscape = 1
        case .landscapeRight: landscape = -1
        case .portrait, .portraitUpsideDown: landscape = 0
        default: break
        }
    }
    private func requestCamera() {
        guard visible, models.state.usable else { return }
        Task {
            if AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined {
                cameraAllowed = await AVCaptureDevice.requestAccess(for: .video)
            } else { cameraAllowed = AVCaptureDevice.authorizationStatus(for: .video) == .authorized }
        }
    }
    private func recognizeFrame(_ data: Data) async {
        guard live, landscape != 0, let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else { return }
        do { await store.recognize(data: data, catalog: bundle, region: profile.server, files: try models.files(), live: true) }
        catch { store.error = tr("识别模型不可用，请检查模型更新后重试。") }
    }
    private func recognizePhoto() async {
        guard let photo, ready, let bundle = catalog.bundle, let profile = personal.snapshot.activeProfile else { return }
        store.beginPhoto()
        do {
            guard let data = try await photo.loadTransferable(type: Data.self) else { reset(); store.error = tr("识别失败"); return }
            try Task.checkCancellation()
            await store.recognize(data: data, catalog: bundle, region: profile.server, files: try models.files(), live: false)
        } catch is CancellationError { }
        catch { reset(); store.error = tr("识别失败") }
    }
    private func openEntry() {
        guard let result = store.result, let song, let profile = personal.snapshot.activeProfile,
              let sheet = song.sheets.first(where: { $0.type == result.match.type && $0.difficulty == result.match.difficulty }) else { return }
        store.invalidate()
        entry = ScannerEntrySelection(song: song, sheet: sheet, result: result, profileID: profile.id, region: profile.server)
    }
    private func reset() { photo = nil; store.reset() }
}

private extension View {
    func scannerOverlayButton() -> some View {
        labelStyle(.iconOnly).font(.system(size: 20, weight: .semibold)).foregroundStyle(.white)
            .padding(10).background(.ultraThinMaterial, in: .circle)
    }
}
