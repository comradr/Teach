package com.example

private val markdownTableSeparator = Regex("^\\s*\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$")
private val inlineMarkdownToken = Regex("""\*\*(.+?)\*\*|`([^`\n]+?)`|\*([^*\n]+?)\*|_([^_\n]+?)_""")

private const val RTF_TABLE_WIDTH_TWIPS = 10200

fun markdownToRtf(markdown: String): String = buildString {
    append("{\\rtf1\\ansi\\ansicpg1251\\uc1\\deff0")
    append("{\\fonttbl{\\f0\\fswiss\\fcharset204 Arial;}{\\f1\\fmodern\\fcharset204 Courier New;}}")
    append("{\\colortbl;\\red0\\green0\\blue0;\\red238\\green233\\blue255;}")
    append("\\viewkind4\\widowctrl")
    append("\\paperw11907\\paperh16840\\margl850\\margr850\\margt850\\margb850")
    append("\\f0\\fs22\n")

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
    if (columns <= 0) return

    val boundaries = rtfColumnBoundaries(columns)
    rows.forEachIndexed { rowIndex, row ->
        append("\\trowd\\trgaph60\\trleft0\\trkeep")
        repeat(columns) { column ->
            append("\\clvertalt")
            append("\\clpadl90\\clpadfl3\\clpadr90\\clpadfr3\\clpadt60\\clpadft3\\clpadb60\\clpadfb3")
            append("\\clbrdrt\\brdrs\\brdrw10\\brdrcf1")
            append("\\clbrdrl\\brdrs\\brdrw10\\brdrcf1")
            append("\\clbrdrb\\brdrs\\brdrw10\\brdrcf1")
            append("\\clbrdrr\\brdrs\\brdrw10\\brdrcf1")
            if (rowIndex == 0) append("\\clcbpat2")
            append("\\cellx${boundaries[column]}")
        }
        append('\n')

        repeat(columns) { column ->
            append("\\pard\\intbl\\ql\\fs18 ")
            if (rowIndex == 0) append("\\b ")
            append(inlineMarkdownToRtf(row.getOrElse(column) { "" }))
            if (rowIndex == 0) append("\\b0 ")
            append("\\cell\n")
        }
        append("\\row\n")
    }
    append("\\pard\\fs22\\par\n")
}

private fun rtfColumnBoundaries(columns: Int): IntArray {
    val weights = when (columns) {
        // Typical lesson-plan table: time / stage / teacher / students / materials.
        5 -> intArrayOf(8, 18, 31, 27, 16)
        4 -> intArrayOf(10, 20, 38, 32)
        3 -> intArrayOf(15, 30, 55)
        2 -> intArrayOf(35, 65)
        else -> null
    }

    return IntArray(columns) { index ->
        if (weights == null) {
            (RTF_TABLE_WIDTH_TWIPS * (index + 1)) / columns
        } else {
            val cumulativePercent = weights.take(index + 1).sum()
            if (index == columns - 1) RTF_TABLE_WIDTH_TWIPS
            else (RTF_TABLE_WIDTH_TWIPS * cumulativePercent) / 100
        }
    }
}

private fun looksLikeTableRow(line: String): Boolean = line.count { it == '|' } >= 2

private fun parseTableRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private fun markdownLineToRtf(line: String): String = when {
    line.startsWith("# ") -> "\\pard\\b\\fs36 ${inlineMarkdownToRtf(line.removePrefix("# "))}\\b0\\fs22"
    line.startsWith("## ") -> "\\pard\\b\\fs32 ${inlineMarkdownToRtf(line.removePrefix("## "))}\\b0\\fs22"
    line.startsWith("### ") -> "\\pard\\b\\fs28 ${inlineMarkdownToRtf(line.removePrefix("### "))}\\b0\\fs22"
    line.startsWith("- ") || line.startsWith("* ") ->
        "\\pard\\fi-360\\li720\\tx720\\bullet\\tab ${inlineMarkdownToRtf(line.drop(2))}\\pard"
    else -> "\\pard ${inlineMarkdownToRtf(line)}"
}

private fun inlineMarkdownToRtf(value: String): String =
    splitExportInlineBreaks(value).joinToString("\\line ") { segment ->
        inlineMarkdownSegmentToRtf(decodeCommonExportEntities(segment))
    }

private fun inlineMarkdownSegmentToRtf(value: String): String = buildString {
    var cursor = 0
    inlineMarkdownToken.findAll(value).forEach { match ->
        if (match.range.first > cursor) {
            append(escapeRtfText(value.substring(cursor, match.range.first)))
        }

        when {
            match.groups[1] != null -> {
                append("\\b ")
                append(escapeRtfText(match.groups[1]!!.value))
                append("\\b0 ")
            }
            match.groups[2] != null -> {
                append("\\f1 ")
                append(escapeRtfText(match.groups[2]!!.value))
                append("\\f0 ")
            }
            match.groups[3] != null -> {
                append("\\i ")
                append(escapeRtfText(match.groups[3]!!.value))
                append("\\i0 ")
            }
            match.groups[4] != null -> {
                append("\\i ")
                append(escapeRtfText(match.groups[4]!!.value))
                append("\\i0 ")
            }
        }
        cursor = match.range.last + 1
    }

    if (cursor < value.length) {
        append(escapeRtfText(value.substring(cursor)))
    }
}

private fun escapeRtfText(value: String): String = buildString {
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '{' -> append("\\{")
            '}' -> append("\\}")
            '\t' -> append("\\tab ")
            '\r' -> Unit
            '\n' -> append("\\line ")
            else -> {
                if (char.code > 127) {
                    val signedCodeUnit = if (char.code > 32767) char.code - 65536 else char.code
                    append("\\u").append(signedCodeUnit).append('?')
                } else {
                    append(char)
                }
            }
        }
    }
}
