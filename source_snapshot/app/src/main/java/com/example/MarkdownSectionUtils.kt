package com.example

data class MarkdownSection(
    val start: Int,
    val end: Int,
    val level: Int,
    val headingLine: String,
    val headingText: String,
    val text: String
)

private val markdownHeadingRegex = Regex("(?m)^(#{1,6})[ \\t]+(.+?)[ \\t]*$")

fun findMarkdownSections(content: String, titleQuery: String): List<MarkdownSection> {
    val query = titleQuery.trim()
    if (query.isEmpty()) return emptyList()

    val headings = markdownHeadingRegex.findAll(content).toList()
    if (headings.isEmpty()) return emptyList()

    val matching = headings.withIndex().filter { (_, match) ->
        match.groupValues[2].trim().contains(query, ignoreCase = true)
    }

    return matching.map { (index, match) ->
        val level = match.groupValues[1].length
        val nextSiblingOrParent = headings.drop(index + 1).firstOrNull {
            it.groupValues[1].length <= level
        }
        val end = nextSiblingOrParent?.range?.first ?: content.length
        val start = match.range.first
        MarkdownSection(
            start = start,
            end = end,
            level = level,
            headingLine = match.value.trimEnd(),
            headingText = match.groupValues[2].trim(),
            text = content.substring(start, end).trimEnd()
        )
    }
}

fun findMarkdownSection(content: String, titleQuery: String): MarkdownSection? {
    val sections = findMarkdownSections(content, titleQuery)
    if (sections.isEmpty()) return null

    val query = titleQuery.trim()
    return sections.firstOrNull { it.headingText.equals(query, ignoreCase = true) }
        ?: sections.first()
}

fun replaceMarkdownSection(content: String, section: MarkdownSection, replacement: String): String {
    var normalized = replacement.trim()
    if (!normalized.startsWith("#")) {
        normalized = section.headingLine + "\n" + normalized
    }

    val suffix = if (section.end < content.length) "\n\n" else ""
    return content.replaceRange(section.start, section.end, normalized.trimEnd() + suffix)
}

fun upsertMarkdownSection(content: String, headingLine: String, body: String): String {
    val normalizedHeading = headingLine.trim()
    val title = normalizedHeading.replace(Regex("^#{1,6}\\s+"), "").trim()
    val replacement = normalizedHeading + "\n" + body.trim()
    val existing = findMarkdownSection(content, title)
    return if (existing != null) {
        replaceMarkdownSection(content, existing, replacement)
    } else {
        content.trimEnd() + "\n\n" + replacement
    }
}
