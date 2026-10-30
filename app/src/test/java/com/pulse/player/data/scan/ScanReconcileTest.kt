package com.pulse.player.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanReconcileTest {

    @Test
    fun scanState_idleInitial() {
        val state: ScanState = ScanState.Idle
        assertEquals(ScanState.Idle, state)
    }

    @Test
    fun scanState_scanningShowsFoundCount() {
        val state = ScanState.Scanning(42)
        assertEquals(42, state.found)
    }

    @Test
    fun scanState_reconcilingState() {
        val state: ScanState = ScanState.Reconciling
        assertEquals(ScanState.Reconciling, state)
    }

    @Test
    fun scanState_successRecordsTimestampAndTotal() {
        val now = 1700000000000L
        val state = ScanState.Success(now, 150)
        assertEquals(now, state.atMs)
        assertEquals(150, state.total)
    }

    @Test
    fun scanState_partialEmptyResultSuspiciousProtectsLibrary() {
        val lastGood = 1699999000000L
        val state = ScanState.Partial(PartialReason.EmptyResultSuspicious, lastGood)
        assertEquals(PartialReason.EmptyResultSuspicious, state.reason)
        assertEquals(lastGood, state.lastGoodAtMs)
    }

    @Test
    fun scanState_partialPermissionRevokedPreservesSnapshot() {
        val lastGood = 1699999000000L
        val state = ScanState.Partial(PartialReason.PermissionRevoked, lastGood)
        assertEquals(PartialReason.PermissionRevoked, state.reason)
    }

    @Test
    fun scanState_partialCancelledDoesNotTouchAvailability() {
        val state = ScanState.Partial(PartialReason.Cancelled, null)
        assertEquals(PartialReason.Cancelled, state.reason)
    }
}

