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
                stream.write(markdownToRtf(plan.content).toByteArray())
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
            if (line.count { it == '|' } >= 2 && index + 1 < lines.size &&
                Regex("^\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?$").matches(lines[index + 1].trim())
            ) {
                closeList()
                val rows = mutableListOf(line.trim('|').split('|').map { it.trim() })
                index += 2
                while (index < lines.size && lines[index].count { it == '|' } >= 2) {
                    rows += lines[index].trim().trim('|').split('|').map { it.trim() }
                    index++
                }
                append("<div class=\"table-wrap\"><table><thead><tr>")
                rows.first().forEach { append("<th>${inlineMarkdownToHtml(it)}</th>") }
                append("</tr></thead><tbody>")
                rows.drop(1).forEach { row ->
                    append("<tr>")
                    row.forEach { append("<td>${inlineMarkdownToHtml(it)}</td>") }
                    append("</tr>")
                }
                append("</tbody></table></div>")
                continue
            }

            when {
                line.isBlank() -> closeList()
                line.startsWith("### ") -> { closeList(); append("<h3>${inlineMarkdownToHtml(line.drop(4))}</h3>") }
                line.startsWith("## ") -> { closeList(); append("<h2>${inlineMarkdownToHtml(line.drop(3))}</h2>") }
                line.startsWith("# ") -> { closeList(); append("<h1>${inlineMarkdownToHtml(line.drop(2))}</h1>") }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (!listOpen) { append("<ul>"); listOpen = true }
                    append("<li>${inlineMarkdownToHtml(line.drop(2))}</li>")
                }
                else -> { closeList(); append("<p>${inlineMarkdownToHtml(line)}</p>") }
            }
            index++
        }
        closeList()
    }

    return """
        <html><head><meta charset="utf-8"><style>
        @page { margin: 14mm; }
        body { font-family: sans-serif; color: #222; font-size: 12pt; line-height: 1.42; }
        h1 { font-size: 22pt; margin: 0 0 12pt; color: #4f36b8; }
        h2 { font-size: 17pt; margin: 18pt 0 8pt; color: #5d43c4; page-break-after: avoid; }
        h3 { font-size: 14pt; margin: 14pt 0 6pt; page-break-after: avoid; }
        p { margin: 5pt 0; } ul { margin: 5pt 0 8pt 18pt; padding: 0; }
        li { margin: 3pt 0; }
        .document-title { font-size: 12pt; color: #6f6a7c; margin-bottom: 12pt; }
        .table-wrap { margin: 10pt 0 14pt; }
        table { width: 100%; border-collapse: collapse; font-size: 9.5pt; page-break-inside: auto; }
        tr { page-break-inside: avoid; } th, td { border: 1px solid #777; padding: 5pt; vertical-align: top; }
        th { background: #eee9ff; font-weight: 700; }
        </style></head><body><div class="document-title">${escapeHtml(title)}</div>$body</body></html>
    """.trimIndent()
}

private fun inlineMarkdownToHtml(value: String): String = escapeHtml(value)
    .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
    .replace(Regex("`(.*?)`"), "<code>$1</code>")

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

internal fun cardsMarkdownToHtml(markdown: String): String {
    var body = extractCardsForPrinting(markdown)
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        .replace(Regex("### (.*?)\n"), "</div><div class=\"card\"><h3>$1</h3>\n")
        .replace(Regex("## (.*?)\n"), "</div><div class=\"card\"><h2>$1</h2>\n")
        .replace(Regex("# (.*?)\n"), "</div><div class=\"card\"><h1>$1</h1>\n")
        .replace(Regex("(?m)^(?:\\*|-) (.*?)(?:\n|$)"), "<li>$1</li>\n")
        .replace(Regex("!\\[(.*?)\\]\\((.*?)\\)"), "<img src=\"$2\" alt=\"$1\">")
        .replace("\n", "<br>")
    body = if (body.startsWith("</div>")) body.substring(6) else "<div class=\"card\">$body"
    body = (body + "</div>").replace("<div class=\"card\"></div>", "")
    return """
        <html><head><meta charset="utf-8"><style>
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
