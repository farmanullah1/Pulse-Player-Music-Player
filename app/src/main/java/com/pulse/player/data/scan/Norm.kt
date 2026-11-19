package com.pulse.player.data.scan

import java.text.Normalizer

private val WHITESPACE_REGEX = Regex("\\s+")

fun norm(s: String?): String =
    Normalizer.normalize((s ?: "").trim(), Normalizer.Form.NFKC).lowercase().replace(WHITESPACE_REGEX, " ")
