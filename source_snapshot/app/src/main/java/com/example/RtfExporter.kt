package com.example

private val markdownTableSeparator = Regex("^\\s*\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$")

fun markdownToRtf(markdown: String): String = buildString {
    append("{\\rtf1\\ansi\\ansicpg1251\\uc1\\deff0{\\fonttbl{\\f0\\fswiss\\fcharset204 Arial;}}\\f0\\fs24\n")
    val lines = markdown.lines()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trimEnd()
        if (looksLikeTableRow(line) && index + 1 < lines.size && markdownTableSeparator.matches(lines[index + 1])) {
            val rows = mutableListOf(parseTableRow(line))
            index += 2
            while (index < lines.size && looksLikeTableRow(lines[index])) {
                rows += parseTableRow(lines[index])
                index++
            }
            appendRtfTable(rows)
            continue
        }
        append(markdownLineToRtf(line)).append("\\par\n")
        index++
    }
    append("}")
}

private fun StringBuilder.appendRtfTable(rows: List<List<String>>) {
    val columns = rows.maxOfOrNull { it.size } ?: return
    val cellWidth = (9000 / columns).coerceAtLeast(1200)
    rows.forEachIndexed { rowIndex, row ->
        append("\\trowd\\trgaph108\\trleft0")
        repeat(columns) { column -> append("\\cellx${cellWidth * (column + 1)}") }
        append('\n')
        repeat(columns) { column ->
            append("\\intbl ")
            if (rowIndex == 0) append("\\b ")
            append(inlineMarkdownToRtf(row.getOrElse(column) { "" }))
            if (rowIndex == 0) append("\\b0 ")
            append("\\cell ")
        }
        append("\\row\n")
    }
    append("\\pard\\par\n")
}

private fun looksLikeTableRow(line: String): Boolean = line.count { it == '|' } >= 2

private fun parseTableRow(line: String): List<String> = line.trim().trim('|').split('|').map { it.trim() }

private fun markdownLineToRtf(line: String): String = when {
    line.startsWith("# ") -> "\\b\\fs36 ${inlineMarkdownToRtf(line.removePrefix("# "))}\\b0\\fs24"
    line.startsWith("## ") -> "\\b\\fs32 ${inlineMarkdownToRtf(line.removePrefix("## "))}\\b0\\fs24"
    line.startsWith("### ") -> "\\b\\fs28 ${inlineMarkdownToRtf(line.removePrefix("### "))}\\b0\\fs24"
    line.startsWith("- ") || line.startsWith("* ") -> "\\bullet  ${inlineMarkdownToRtf(line.drop(2))}"
    else -> inlineMarkdownToRtf(line)
}

private fun inlineMarkdownToRtf(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .map { char -> if (char.code > 127) "\\u${char.code}?" else char.toString() }
        .joinToString("")
    return escaped.replace(Regex("\\*\\*(.*?)\\*\\*")) { "\\b ${it.groupValues[1]}\\b0 " }
}
