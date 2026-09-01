package com.example

object LessonTextSanitizer {
    fun sanitizeGeminiMarkdown(text: String): String {
        var sanitized = text

        // 1. Remove block and inline display math brackets: \[ ... \] and \( ... \)
        sanitized = sanitized.replace(Regex("""\\\[(.*?)\\\]""", RegexOption.DOT_MATCHES_ALL), "$1")
        sanitized = sanitized.replace(Regex("""\\\((.*?)\\\)""", RegexOption.DOT_MATCHES_ALL), "$1")

        // 2. Remove $...$ and $$...$$
        // We want to remove pairs of $.
        // $$...$$
        sanitized = sanitized.replace(Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL), "$1")
        // $...$ (ensure we don't match standard currency $5 by requiring closing $)
        sanitized = sanitized.replace(Regex("""\$([^$\n]+?)\$"""), "$1")

        // 3. Replace LaTeX fractions: \frac{a}{b} -> a/b
        // Using a loop to handle nested/multiple occurrences.
        val fracRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
        while (fracRegex.containsMatchIn(sanitized)) {
            sanitized = sanitized.replace(fracRegex, "$1/$2")
        }

        // 4. Replace other math functions
        sanitized = sanitized.replace(Regex("""\\text\{([^{}]*)\}"""), "$1")
        sanitized = sanitized.replace(Regex("""\\boxed\{([^{}]*)\}"""), "$1")
        
        // 5. Direct replacements
        sanitized = sanitized.replace("\\times", "×")
        sanitized = sanitized.replace("\\div", "÷")
        sanitized = sanitized.replace("\\cdot", "·")
        sanitized = sanitized.replace("\\pm", "±")
        sanitized = sanitized.replace("\\neq", "≠")
        // Longer commands must be replaced first: otherwise \leq would become ≤q.
        sanitized = sanitized.replace("\\leq", "≤")
        sanitized = sanitized.replace("\\le", "≤")
        sanitized = sanitized.replace("\\geq", "≥")
        sanitized = sanitized.replace("\\ge", "≥")
        sanitized = sanitized.replace("\\approx", "≈")
        sanitized = sanitized.replace("\\rightarrow", "→")

        // 6. Remove left/right formatting
        sanitized = sanitized.replace("\\left", "")
        sanitized = sanitized.replace("\\right", "")

        return sanitized
    }
}
