package com.example

fun extractCardsForPrinting(content: String): String {
    val cardSections = findMarkdownSections(content, "Карточка")
    val independentSections = findMarkdownSections(content, "Материалы для самостоятельной работы")

    // Prefer explicit printable cards. Older plans may only have a general
    // "Материалы для самостоятельной работы" section, so keep it as fallback.
    val sections = (if (cardSections.isNotEmpty()) cardSections else independentSections)
        .distinctBy { it.start }
        .sortedBy { it.start }

    return if (sections.isNotEmpty()) {
        sections.joinToString("\n\n<hr>\n\n") { it.text.trim() }
    } else {
        content
    }
}
