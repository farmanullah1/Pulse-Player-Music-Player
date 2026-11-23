package com.pulse.player.playback

import com.pulse.player.data.scan.ReplayGainTagReader
import com.pulse.player.data.settings.ReplayGainMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ReplayGainAndCrossfadeTest {

    @Test
    fun parseGainDb_handlesStandardCommaAndWhitespaceFormats() {
        assertEquals(-6.42f, ReplayGainCalculator.parseGainDb("-6.42 dB")!!, 0.001f)
        assertEquals(3.15f, ReplayGainCalculator.parseGainDb("+3,15 DB")!!, 0.001f)
        assertEquals(-24f, ReplayGainCalculator.parseGainDb("-40.0 dB")!!, 0.001f)
        assertEquals(24f, ReplayGainCalculator.parseGainDb("+32.0 dB")!!, 0.001f)
        assertNull(ReplayGainCalculator.parseGainDb(""))
        assertNull(ReplayGainCalculator.parseGainDb("no gain here"))
    }

    @Test
    fun compute_respectsOffTrackAlbumAndPreampWithSafeBoostClamp() {
        val off = ReplayGainCalculator.compute(ReplayGainMode.OFF, -7.5f, -5.0f, 3.0f)
        assertEquals(1f, off.volumeScale, 0.0001f)
        assertEquals(0, off.loudnessBoostMb)
        assertEquals(0f, off.appliedDb, 0.0001f)

        val trackMode = ReplayGainCalculator.compute(ReplayGainMode.TRACK, -6.0f, -2.0f, 0f)
        assertEquals(10f.pow(-6.0f / 20f), trackMode.volumeScale, 0.001f)
        assertEquals(0, trackMode.loudnessBoostMb)
        assertEquals(-6.0f, trackMode.appliedDb, 0.001f)

        // Album falls back to track gain when album gain is absent (0f)
        val albumFallback = ReplayGainCalculator.compute(ReplayGainMode.ALBUM, -4.0f, 0f, 0f)
        assertEquals(-4.0f, albumFallback.appliedDb, 0.001f)

        // Positive effective gain uses LoudnessEnhancer millibels capped at +600 mB (+6 dB)
        val positiveBoost = ReplayGainCalculator.compute(ReplayGainMode.TRACK, 2.0f, 0f, 6.0f)
        assertEquals(1f, positiveBoost.volumeScale, 0.0001f)
        assertEquals(600, positiveBoost.loudnessBoostMb)
        assertEquals(6.0f, positiveBoost.appliedDb, 0.001f)
    }

    @Test
    fun crossfadeGains_equalPowerMaintainsConstantPowerSum() {
        val start = ReplayGainCalculator.crossfadeGains(0f, equalPower = true)
        assertEquals(1f, start.outgoing, 0.001f)
        assertEquals(0f, start.incoming, 0.001f)

        val mid = ReplayGainCalculator.crossfadeGains(0.5f, equalPower = true)
        val energySum = mid.outgoing * mid.outgoing + mid.incoming * mid.incoming
        assertEquals(1f, energySum, 0.001f)

        val end = ReplayGainCalculator.crossfadeGains(1f, equalPower = true)
        assertEquals(0f, end.outgoing, 0.001f)
        assertEquals(1f, end.incoming, 0.001f)

        val linearMid = ReplayGainCalculator.crossfadeGains(0.25f, equalPower = false)
        assertEquals(0.75f, linearMid.outgoing, 0.001f)
        assertEquals(0.25f, linearMid.incoming, 0.001f)
    }

    @Test
    fun replayGainTagReader_parsesAsciiVorbisAndUtf16LeId3TxxxHeaders() {
        val vorbisHeader = (
            "OggS\u0000\u0002vorbis" +
                "REPLAYGAIN_TRACK_GAIN=-7.25 dB\u0000" +
                "REPLAYGAIN_ALBUM_GAIN=-5.80 dB\u0000"
            ).toByteArray(Charsets.US_ASCII)
        val vorbisTags = ReplayGainTagReader.parseFromBytes(vorbisHeader)
        assertEquals(-7.25f, vorbisTags.trackGainDb, 0.01f)
        assertEquals(-5.80f, vorbisTags.albumGainDb, 0.01f)

        val id3Key = "REPLAYGAIN_TRACK_GAIN".toByteArray(Charsets.UTF_16LE)
        val id3Val = "-4.10 dB".toByteArray(Charsets.UTF_16LE)
        val utf16Payload = ByteArray(16 + id3Key.size + 2 + id3Val.size + 4)
        System.arraycopy(id3Key, 0, utf16Payload, 10, id3Key.size)
        System.arraycopy(id3Val, 0, utf16Payload, 10 + id3Key.size + 2, id3Val.size)
        val id3Tags = ReplayGainTagReader.parseFromBytes(utf16Payload)
        assertEquals(-4.10f, id3Tags.trackGainDb, 0.01f)

        val empty = ReplayGainTagReader.parseFromBytes(byteArrayOf())
        assertEquals(0f, empty.trackGainDb, 0.001f)
        assertEquals(0f, empty.albumGainDb, 0.001f)
        assertTrue(ReplayGainTagReader.parseFromBytes("random audio bytes".toByteArray()).trackGainDb == 0f)
    }
}
