package com.pulse.player.data.scan

import java.text.Normalizer

fun norm(s: String?): String =
    Normalizer.normalize((s ?: "").trim(), Normalizer.Form.NFKC).lowercase().replace(Regex("\\s+"), " ")
