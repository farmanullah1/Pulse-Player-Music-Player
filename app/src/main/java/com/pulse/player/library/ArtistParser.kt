package com.pulse.player.library

import com.pulse.player.data.scan.norm

/**
 * Parses artist strings into individual artists according to configurable delimiter rules.
 *
 * Handles common patterns:
 * - Delimiters: `;`, `,`, ` / `, ` & `, ` ft. `, ` ft `, ` feat. `, ` feat `, ` featuring `
 * - Exceptions list: artist names that must NOT be split even if they contain delimiters
 *   (e.g. "AC/DC", "Tyler, The Creator", "Earth, Wind & Fire")
 *
 * Implements NAM-L2 and NAM-L8 from the Feature Gap Matrix.
 */
object ArtistParser {

    /** Default delimiter patterns (ordered from most specific to least). */
    val DEFAULT_DELIMITERS = listOf(
        " featuring ",
        " feat. ",
        " feat ",
        " ft. ",
        " ft ",
        " vs. ",
        " vs ",
        " & ",
        " / ",
        " and ",
        ";",
        ","
    )

    /**
     * Default exceptions — names that contain delimiter characters but must not be split.
     * Users may add their own exceptions via settings.
     */
    val DEFAULT_EXCEPTIONS = setOf(
        "ac/dc",
        "earth, wind & fire",
        "tyler, the creator",
        "rage against the machine",
        "of monsters and men",
        "florence + the machine",
        "simon & garfunkel",
        "hall & oates",
        "guns n' roses",
        "crosby, stills, nash & young"
    )

    /**
     * Split an artist string into individual artist names.
     *
     * @param raw            The raw artist string from file tags or MediaStore.
     * @param delimiters     Delimiter patterns to split on.
     * @param exceptions     Normalised artist names that must not be split.
     * @return               Ordered list of individual artist names, trimmed and deduplicated.
     */
    fun split(
        raw: String,
        delimiters: List<String> = DEFAULT_DELIMITERS,
        exceptions: Set<String> = DEFAULT_EXCEPTIONS
    ): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()

        // Check if the whole string matches an exception (case-insensitive normalised)
        if (exceptions.contains(norm(trimmed))) return listOf(trimmed)

        // Try each delimiter in order
        for (delimiter in delimiters) {
            val parts = trimmed.split(delimiter, ignoreCase = true)
            if (parts.size <= 1) continue

            // If splitting would break an exception name, skip this delimiter
            val wouldBreakException = parts.any { part ->
                exceptions.any { ex ->
                    // Check if part is a fragment of an exception
                    norm(trimmed).contains(ex) && (norm(part) != ex) &&
                    exceptions.contains(norm(part).trim()).not() &&
                    ex.contains(norm(part))
                }
            }
            if (wouldBreakException) continue

            // Recursively split each part with remaining delimiters
            val remainingDelimiters = delimiters.drop(delimiters.indexOf(delimiter) + 1)
            return parts
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .flatMap { split(it, remainingDelimiters, exceptions) }
                .distinct()
        }

        return listOf(trimmed)
    }

    /**
     * Extract featured artist information from a track title.
     * Finds patterns like "Song Title (feat. Artist Name)" or "Song Title ft. Artist".
     *
     * @return Pair of (clean title, list of featured artists); or (original title, empty list) if no match.
     */
    fun extractFeatured(title: String): Pair<String, List<String>> {
        if (title.isBlank()) return title to emptyList()

        val patterns = listOf(
            Regex("""\s*\((?:feat|ft|featuring)\.?\s+([^)]+)\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\[(?:feat|ft|featuring)\.?\s+([^\]]+)]""", RegexOption.IGNORE_CASE),
            Regex("""\s+(?:feat|ft|featuring)\.?\s+(.+)$""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(title) ?: continue
            val cleanTitle = title.substring(0, match.range.first).trim()
            val featuredStr = match.groupValues[1].trim()
            val featuredArtists = split(featuredStr)
            if (featuredArtists.isNotEmpty()) {
                return cleanTitle to featuredArtists
            }
        }

        return title to emptyList()
    }

    /**
     * Split a genre string into individual genres using common delimiters.
     * Genre separators are typically simpler: `;`, `,`, `/`.
     */
    fun splitGenres(
        raw: String,
        delimiters: List<String> = listOf(";", ",", "/")
    ): List<String> {
        if (raw.isBlank()) return emptyList()
        var parts = listOf(raw)
        for (delimiter in delimiters) {
            parts = parts.flatMap { it.split(delimiter) }
        }
        return parts.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
}
