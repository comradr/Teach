package com.example

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.LessonPlanEntity
import com.example.data.local.PlannerDatabase
import kotlinx.coroutines.launch

private val printTableSeparator = Regex("^\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?$")
private val htmlInlineMarkdownToken = Regex("""\*\*(.+?)\*\*|`([^`\n]+?)`|\*([^*\n]+?)\*|_([^_\n]+?)_""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanDocumentActions(plan: LessonPlanEntity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/rtf")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(markdownToRtf(plan.content).toByteArray(Charsets.US_ASCII))
            }
        }
    }
    val webView = remember(context) { WebView(context) }
    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, plan.content)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Поделиться планом"))
        }) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Поделиться")
        }

        OutlinedButton(onClick = {
            createDocument.launch("${plan.title.replace(" ", "_")}.rtf")
        }) { Text("Документ RTF") }

        OutlinedButton(onClick = {
            scope.launch {
                PlannerDatabase.getDatabase(context).plannerDao().insertLessonPlan(
                    plan.copy(id = 0, title = "${plan.title} (Копия)", timestamp = System.currentTimeMillis())
                )
                Toast.makeText(context, "План скопирован", Toast.LENGTH_SHORT).show()
            }
        }) { Text("Клонировать") }

        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, plan.title)
                putExtra(CalendarContract.Events.DESCRIPTION, plan.content.take(500))
            })
        }) {
            Icon(Icons.Default.DateRange, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("В календарь")
        }

        Button(onClick = {
            printHtml(
                context = context,
                webView = webView,
                jobName = "${plan.title} — полный план",
                html = fullPlanMarkdownToHtml(plan.title, plan.content)
            )
        }) {
            Text("Весь план (PDF/Печать)")
        }

        OutlinedButton(onClick = { printCards(context, webView, plan.content) }) {
            Text("Карточки (PDF/Печать)")
        }
    }
}

internal fun fullPlanMarkdownToHtml(title: String, markdown: String): String {
    // Do not replace <br> globally: it may be inside a Markdown table cell and must not
    // split the logical table row. inlineMarkdownToHtml handles it as a native HTML break.
    val lines = markdown.lines()
    val body = buildString {
        var index = 0
        var listOpen = false

        fun closeList() {
            if (listOpen) {
                append("</ul>")
                listOpen = false
            }
        }

        while (index < lines.size) {
            val line = lines[index].trim()
            if (
                line.count { it == '|' } >= 2 &&
                index + 1 < lines.size &&
                printTableSeparator.matches(lines[index + 1].trim())
            ) {
                closeList()
                val rows = mutableListOf(line.trim('|').split('|').map { it.trim() })
                index += 2
                while (index < lines.size && lines[index].count { it == '|' } >= 2) {
                    rows += lines[index].trim().trim('|').split('|').map { it.trim() }
                    index++
                }

                val columns = rows.maxOfOrNull { it.size } ?: 0
                append("<div class=\"table-wrap\"><table>")
                appendHtmlColGroup(columns)
                append("<thead><tr>")
                repeat(columns) { column ->
                    append("<th>${inlineMarkdownToHtml(rows.first().getOrElse(column) { "" })}</th>")
                }
                append("</tr></thead><tbody>")
                rows.drop(1).forEach { row ->
                    append("<tr>")
                    repeat(columns) { column ->
                        append("<td>${inlineMarkdownToHtml(row.getOrElse(column) { "" })}</td>")
                    }
                    append("</tr>")
                }
                append("</tbody></table></div>")
                continue
            }

            when {
                line.isBlank() -> closeList()
                line.startsWith("### ") -> {
                    closeList()
                    append("<h3>${inlineMarkdownToHtml(line.drop(4))}</h3>")
                }
                line.startsWith("## ") -> {
                    closeList()
                    append("<h2>${inlineMarkdownToHtml(line.drop(3))}</h2>")
                }
                line.startsWith("# ") -> {
                    closeList()
                    append("<h1>${inlineMarkdownToHtml(line.drop(2))}</h1>")
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (!listOpen) {
                        append("<ul>")
                        listOpen = true
                    }
                    append("<li>${inlineMarkdownToHtml(line.drop(2))}</li>")
                }
                else -> {
                    closeList()
                    append("<p>${inlineMarkdownToHtml(line)}</p>")
                }
            }
            index++
        }
        closeList()
    }

    return """
        <html lang="ru"><head><meta charset="utf-8"><style>
        @page { size: A4 portrait; margin: 12mm; }
        * { box-sizing: border-box; }
        body {
            font-family: sans-serif;
            color: #222;
            font-size: 10.5pt;
            line-height: 1.36;
            margin: 0;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }
        h1 { font-size: 20pt; margin: 0 0 10pt; color: #4f36b8; }
        h2 { font-size: 15.5pt; margin: 15pt 0 7pt; color: #5d43c4; page-break-after: avoid; break-after: avoid; }
        h3 { font-size: 13pt; margin: 12pt 0 5pt; page-break-after: avoid; break-after: avoid; }
        p { margin: 4pt 0; }
        ul { margin: 5pt 0 8pt 18pt; padding: 0; }
        li { margin: 2pt 0; }
        code { font-family: monospace; font-size: 0.95em; }
        .document-title { font-size: 10pt; color: #6f6a7c; margin-bottom: 10pt; }
        .table-wrap { margin: 8pt 0 12pt; width: 100%; }
        table {
            width: 100%;
            border-collapse: collapse;
            table-layout: fixed;
            font-size: 8.7pt;
            line-height: 1.28;
            page-break-inside: auto;
        }
        thead { display: table-header-group; }
        tr { page-break-inside: avoid; break-inside: avoid; }
        th, td {
            border: 1px solid #666;
            padding: 3.5pt;
            vertical-align: top;
            text-align: left;
            word-break: normal;
            overflow-wrap: break-word;
        }
        th { background: #eee9ff; font-weight: 700; }
        </style></head><body><div class="document-title">${escapeHtml(title)}</div>$body</body></html>
    """.trimIndent()
}

private fun StringBuilder.appendHtmlColGroup(columns: Int) {
    if (columns <= 0) return
    val weights = when (columns) {
        // Lesson-plan table: time / stage / teacher / students / materials.
        5 -> intArrayOf(8, 18, 31, 27, 16)
        4 -> intArrayOf(10, 20, 38, 32)
        3 -> intArrayOf(15, 30, 55)
        2 -> intArrayOf(35, 65)
        else -> null
    }

    append("<colgroup>")
    repeat(columns) { column ->
        val width = weights?.get(column) ?: (100.0 / columns)
        append("<col style=\"width:")
        append(if (width is Int) width.toString() else String.format(java.util.Locale.US, "%.3f", width))
        append("%\">")
    }
    append("</colgroup>")
}

private fun inlineMarkdownToHtml(value: String): String =
    splitExportInlineBreaks(value).joinToString("<br>") { segment ->
        inlineMarkdownSegmentToHtml(decodeCommonExportEntities(segment))
    }

private fun inlineMarkdownSegmentToHtml(value: String): String = buildString {
    var cursor = 0
    htmlInlineMarkdownToken.findAll(value).forEach { match ->
        if (match.range.first > cursor) {
            append(escapeHtml(value.substring(cursor, match.range.first)))
        }

        when {
            match.groups[1] != null -> append("<b>${escapeHtml(match.groups[1]!!.value)}</b>")
            match.groups[2] != null -> append("<code>${escapeHtml(match.groups[2]!!.value)}</code>")
            match.groups[3] != null -> append("<i>${escapeHtml(match.groups[3]!!.value)}</i>")
            match.groups[4] != null -> append("<i>${escapeHtml(match.groups[4]!!.value)}</i>")
        }
        cursor = match.range.last + 1
    }

    if (cursor < value.length) {
        append(escapeHtml(value.substring(cursor)))
    }
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

internal fun cardsMarkdownToHtml(markdown: String): String {
    var body = exportBreakTagRegex.replace(extractCardsForPrinting(markdown), "\n")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        .replace(Regex("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)"), "<i>$1</i>")
        .replace(Regex("(?<!_)_([^_\\n]+?)_(?!_)"), "<i>$1</i>")
        .replace(Regex("### (.*?)\n"), "</div><div class=\"card\"><h3>$1</h3>\n")
        .replace(Regex("## (.*?)\n"), "</div><div class=\"card\"><h2>$1</h2>\n")
        .replace(Regex("# (.*?)\n"), "</div><div class=\"card\"><h1>$1</h1>\n")
        .replace(Regex("(?m)^(?:\\*|-) (.*?)(?:\n|$)"), "<li>$1</li>\n")
        .replace(Regex("!\\[(.*?)\\]\\((.*?)\\)"), "<img src=\"$2\" alt=\"$1\">")
        .replace("\n", "<br>")
    body = if (body.startsWith("</div>")) body.substring(6) else "<div class=\"card\">$body"
    body = (body + "</div>").replace("<div class=\"card\"></div>", "")
    return """
        <html lang="ru"><head><meta charset="utf-8"><style>
        body { font-family: sans-serif; padding: 20px; font-size: 18px; background: #fff; }
        .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
        .card { border: 2px dashed #999; padding: 20px; border-radius: 12px; page-break-inside: avoid; }
        h1, h2, h3 { margin-top: 0; color: #333; }
        @media print { body { padding: 0; } .card { border-color: #000; } }
        </style></head><body><h2>Раздаточные материалы</h2><div class="grid">$body</div></body></html>
    """.trimIndent()
}

private fun printCards(context: Context, webView: WebView, markdown: String) {
    printHtml(context, webView, "Карточки к уроку", cardsMarkdownToHtml(markdown))
}

private fun printHtml(context: Context, webView: WebView, jobName: String, html: String) {
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val manager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            manager.print(
                jobName,
                view.createPrintDocumentAdapter(jobName),
                android.print.PrintAttributes.Builder().build()
            )
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}
