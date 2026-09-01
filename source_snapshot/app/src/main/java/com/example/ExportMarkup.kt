package com.example

/**
 * Small compatibility layer for markup that Gemini or older saved plans may contain.
 * We intentionally keep <br> inside the logical Markdown line so table parsing is not broken;
 * renderers turn it into their own native line-break representation later.
 */
internal val exportBreakTagRegex = Regex("(?i)<br\\s*/?>")

internal fun splitExportInlineBreaks(value: String): List<String> =
    exportBreakTagRegex.split(value)

internal fun decodeCommonExportEntities(value: String): String = value
    .replace("&nbsp;", " ", ignoreCase = true)
    .replace("&lt;", "<", ignoreCase = true)
    .replace("&gt;", ">", ignoreCase = true)
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&apos;", "'", ignoreCase = true)
    // Decode ampersand last so text such as &amp;lt; is decoded only once.
    .replace("&amp;", "&", ignoreCase = true)
