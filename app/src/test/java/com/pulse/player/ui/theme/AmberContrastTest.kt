package com.pulse.player.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that the Atmospheric Queue amber accent tokens meet WCAG AA contrast (≥ 4.5:1).
 *
 * C15 in the conflict-resolution table requires:
 *  - `ActiveAmberDark` (#D4AF37) on the dark queue surface (#020706 — Atmospheric Queue bg)
 *  - `ActiveAmberOnLight` (#8A6D00) on the light queue card (#F9F9FB)
 *
 * Both must achieve ≥ 4.5:1 relative contrast ratio.
 */
class AmberContrastTest {

    /** WCAG relative luminance per IEC 61966-2-1. */
    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.04045) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    /** WCAG contrast ratio. */
    private fun contrast(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        val lighter = maxOf(l1, l2)
        val darker  = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun activeAmberDarkOnDarkAtmosphericSurface_meetsWcagAA() {
        // Background: Atmospheric Queue bottom gradient stop #020706
        val bg = Color(0xFF020706)
        val ratio = contrast(Pulse.ActiveAmberDark, bg)
        assertTrue(
            "ActiveAmberDark (#D4AF37) on dark surface must be ≥ 4.5:1, was %.2f:1".format(ratio),
            ratio >= 4.5
        )
    }

    @Test
    fun activeAmberOnLightCard_meetsWcagAA() {
        // Background: light queue card ~#F9F9FB
        val bg = Color(0xFFF9F9FB)
        val ratio = contrast(Pulse.ActiveAmberOnLight, bg)
        assertTrue(
            "ActiveAmberOnLight (#8A6D00) on light card must be ≥ 4.5:1, was %.2f:1".format(ratio),
            ratio >= 4.5
        )
    }

    @Test
    fun activeAmberDarkOnPulseMainBackground_meetsWcagAA() {
        // Background: main Pulse bg #05040A
        val ratio = contrast(Pulse.ActiveAmberDark, Pulse.Bg)
        assertTrue(
            "ActiveAmberDark on Pulse.Bg must be ≥ 4.5:1, was %.2f:1".format(ratio),
            ratio >= 4.5
        )
    }
}
