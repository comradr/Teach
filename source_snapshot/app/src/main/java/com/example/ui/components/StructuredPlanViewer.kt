package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.MarkdownViewer

@Composable
fun StructuredPlanViewer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val sections = splitPlanSections(markdown)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { section ->
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                val containsTable = section.lineSequence().count { it.trimStart().startsWith("|") } >= 2
                if (containsTable) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        MarkdownViewer(
                            markdown = section,
                            modifier = Modifier.widthIn(min = 760.dp)
                        )
                    }
                } else {
                    MarkdownViewer(markdown = section)
                }
            }
        }
    }
}

internal fun splitPlanSections(markdown: String): List<String> {
    if (markdown.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val current = mutableListOf<String>()
    markdown.lines().forEach { line ->
        val isHeading = line.matches(Regex("^#{1,3}\\s+.+"))
        if (isHeading && current.any { it.isNotBlank() }) {
            result += current.joinToString("\n").trim()
            current.clear()
        }
        current += line
    }
    if (current.any { it.isNotBlank() }) {
        result += current.joinToString("\n").trim()
    }
    return result.filter { it.isNotBlank() }
}
