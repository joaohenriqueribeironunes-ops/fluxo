package com.lambda.aiengine

internal object FtsQuery {
    fun sanitize(input: String): String {
        // Remove FTS operators/chars that commonly break MATCH; keep tokens.
        val tokens = Regex("[\\p{L}\\p{N}]+").findAll(input).map { it.value }.filter { it.length >= 2 }.toList()
        if (tokens.isEmpty()) return ""
        // Use simple AND-ish matching via space (FTS5 default) with prefix on last token for UX
        return tokens.take(8).joinToString(" ") { "\"$it\"" }
    }
}
