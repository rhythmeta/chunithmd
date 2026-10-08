package org.rhythmeta.chunithmd.scanner

import android.view.Surface
import org.rhythmeta.chunithmd.shared.scanner.ScanBox

/** OrientationEventListener's clockwise degrees use the opposite convention from Surface rotation. */
internal fun landscapeTargetRotation(orientation: Int): Int? = when (orientation) {
    in 45..134 -> Surface.ROTATION_270
    in 225..314 -> Surface.ROTATION_90
    else -> null
}

/** Keep the last orientation near diagonals or while lying flat, matching the iOS gravity dead band. */
internal fun scannerTargetRotation(orientation: Int, previous: Int): Int = when (orientation) {
    in 0..29, in 330..359 -> Surface.ROTATION_0
    in 60..119 -> Surface.ROTATION_270
    in 150..209 -> Surface.ROTATION_180
    in 240..299 -> Surface.ROTATION_90
    else -> previous
}

internal fun rotateScanBoxForPreview(box: ScanBox, degrees: Int): ScanBox = when (degrees) {
    90 -> box.copy(x = 1 - box.y - box.height, y = box.x, width = box.height, height = box.width)
    180 -> box.copy(x = 1 - box.x - box.width, y = 1 - box.y - box.height)
    270 -> box.copy(x = box.y, y = 1 - box.x - box.width, width = box.height, height = box.width)
    else -> box
}
