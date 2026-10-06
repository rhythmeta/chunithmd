import SwiftUI
import Observation

@MainActor @Observable
final class SongNavigation {
    var song: CatalogSongViewData?
    var preferredSheet: String?
    var sourceID: UUID?
    var source = CGRect.zero
    var target = CGRect.zero
    var progress = 0.0
    var preparing = false
    var flying = false
    var interactive = false
    var returning = false
    var sourceRadius = 0.0
    var reduceMotion = false

    func open(_ song: CatalogSongViewData, from rect: CGRect, radius: Double = 0, sheetID: String? = nil, sourceID: UUID? = nil) {
        guard self.song == nil else { return }
        preferredSheet = sheetID
        self.sourceID = sourceID
        source = rect; target = rect; sourceRadius = radius
        progress = 0; preparing = true; flying = true; interactive = false; returning = false
        self.song = song
    }

    func arrived(at rect: CGRect) {
        target = rect
        guard preparing, rect.width > 0 else { return }
        preparing = false
        withAnimation(reduceMotion ? .easeOut(duration: 0.18) : .smooth(duration: 0.4)) { progress = 1 } completion: {
            if !self.returning && !self.interactive { self.flying = false }
        }
    }

    func dragBack(distance: Double, width: Double) {
        guard !preparing, !returning else { return }
        interactive = true; flying = true
        progress = max(0, min(1, 1 - distance / max(1, width)))
    }

    func finishBack(commit: Bool) {
        guard interactive else { return }
        if commit { close() }
        else {
            interactive = false
            withAnimation(reduceMotion ? .easeOut(duration: 0.18) : .spring(duration: 0.35, bounce: 0)) { progress = 1 } completion: {
                if !self.returning { self.flying = false }
            }
        }
    }

    func close() {
        guard !returning else { return }
        returning = true
        flying = true
        interactive = false
        withAnimation(reduceMotion ? .easeOut(duration: 0.18) : .spring(duration: 0.4, bounce: 0)) {
            progress = 0
        } completion: {
            self.song = nil
            self.returning = false
        }
    }
}
