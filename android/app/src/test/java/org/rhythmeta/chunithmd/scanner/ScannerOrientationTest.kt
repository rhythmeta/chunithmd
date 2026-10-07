package org.rhythmeta.chunithmd.scanner

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.rhythmeta.chunithmd.shared.scanner.ScanBox

class ScannerOrientationTest {
    @Test fun bothLandscapeDirectionsKeepFramesUpright() {
        assertEquals(Surface.ROTATION_270, landscapeTargetRotation(90))
        assertEquals(Surface.ROTATION_90, landscapeTargetRotation(270))
        for (angle in listOf(-1, 0, 44, 135, 180, 224, 315, 359)) assertNull(landscapeTargetRotation(angle))
    }

    @Test fun landscapeBoxesRotateBackIntoPortraitPreviewCoordinates() {
        val box = ScanBox(.1f, .2f, .3f, .4f)
        val clockwise = rotateScanBoxForPreview(box, 90)
        assertEquals(.4f, clockwise.x, .00001f)
        assertEquals(.1f, clockwise.y, .00001f)
        assertEquals(.4f, clockwise.width, .00001f)
        assertEquals(.3f, clockwise.height, .00001f)
        val counterclockwise = rotateScanBoxForPreview(box, 270)
        assertEquals(.2f, counterclockwise.x, .00001f)
        assertEquals(.6f, counterclockwise.y, .00001f)
        val roundTrip = rotateScanBoxForPreview(clockwise, 270)
        assertEquals(box.x, roundTrip.x, .00001f)
        assertEquals(box.y, roundTrip.y, .00001f)
    }
}
