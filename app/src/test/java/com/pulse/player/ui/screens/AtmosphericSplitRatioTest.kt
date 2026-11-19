package com.pulse.player.ui.screens

import com.pulse.player.ui.screens.layouts.DEFAULT_UPPER_SPLIT_RATIO
import com.pulse.player.ui.screens.layouts.MAX_UPPER_SPLIT_RATIO
import com.pulse.player.ui.screens.layouts.MIN_UPPER_SPLIT_RATIO
import com.pulse.player.ui.screens.layouts.clampSplitUpperRatio
import com.pulse.player.ui.screens.layouts.snapSplitUpperRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtmosphericSplitRatioTest {
    @Test
    fun defaultSplitIsFortyPercentTopSixtyPercentBottom() {
        assertEquals(0.40f, DEFAULT_UPPER_SPLIT_RATIO, 0.0001f)
        assertEquals(0.60f, 1f - DEFAULT_UPPER_SPLIT_RATIO, 0.0001f)
    }

    @Test
    fun splitRatioClampsToAllowedBounds() {
        assertEquals(MIN_UPPER_SPLIT_RATIO, clampSplitUpperRatio(0.10f), 0.0001f)
        assertEquals(MAX_UPPER_SPLIT_RATIO, clampSplitUpperRatio(0.95f), 0.0001f)
        assertEquals(0.52f, clampSplitUpperRatio(0.52f), 0.0001f)
    }

    @Test
    fun splitRatioSnapsLightlyNearDetentsAndPreservesFreeDragElsewhere() {
        val (snapped40, didSnap40) = snapSplitUpperRatio(0.412f)
        assertTrue(didSnap40)
        assertEquals(0.40f, snapped40, 0.0001f)

        val (snapped50, didSnap50) = snapSplitUpperRatio(0.491f)
        assertTrue(didSnap50)
        assertEquals(0.50f, snapped50, 0.0001f)

        val (snapped60, didSnap60) = snapSplitUpperRatio(0.610f)
        assertTrue(didSnap60)
        assertEquals(0.60f, snapped60, 0.0001f)

        val (freeRatio, didSnapFree) = snapSplitUpperRatio(0.55f)
        assertFalse(didSnapFree)
        assertEquals(0.55f, freeRatio, 0.0001f)
    }
}
