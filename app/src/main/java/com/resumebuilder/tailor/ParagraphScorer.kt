package com.resumebuilder.tailor

import java.util.Locale

object ParagraphScorer {

    private val emailRegex = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("(\\+?\\d[\\d\\s().-]{7,}\\d)")
    private val urlRegex = Regex("https?://\\S+|linkedin\\.com/\\S+|github\\.com/\\S+", RegexOption.IGNORE_CASE)

    fun isProtected(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        if (emailRegex.containsMatchIn(t)) return true
        if (phoneRegex.containsMatchIn(t)) return true
        if (urlRegex.containsMatchIn(t)) return true
        if (looksLikeNameLine(t.lowercase(Locale.US))) return true
        return false
    }

    fun keywordSet(ctx: JobContext): Set<String> {
        val parts = buildList {
            addAll(tokenize(ctx.jobTitle))
            addAll(tokenize(ctx.companyName))
            addAll(tokenize(ctx.jobDescription))
        }
        return parts
            .map { it.lowercase(Locale.US) }
            .filter { it.length > 2 }
            .toSet()
    }

    fun score(text: String, keywords: Set<String>): Double {
        val lower = text.lowercase(Locale.US)
        if (text.isBlank()) return -500.0
        if (emailRegex.containsMatchIn(text)) return 1_000.0
        if (phoneRegex.containsMatchIn(text)) return 950.0
        if (urlRegex.containsMatchIn(text)) return 900.0
        if (looksLikeNameLine(lower)) return 850.0

        val tokens = tokenize(text).map { it.lowercase(Locale.US) }.filter { it.length > 2 }.toSet()
        var score = tokens.count { it in keywords } * 12.0
        if (looksLikeSectionHeader(lower)) score += 25.0
        if (looksLikeBullet(lower)) score += 8.0
        if (containsYearRange(lower)) score += 6.0
        if (genericBoilerplate(lower)) score -= 40.0
        score -= lower.length / 400.0
        return score
    }

    private fun tokenize(s: String): List<String> =
        s.split(Regex("[^A-Za-z0-9+#]+")).filter { it.isNotBlank() }

    private fun looksLikeSectionHeader(lower: String): Boolean {
        val t = lower.trim()
        if (t.length in 3..48 && t == t.uppercase(Locale.US) && t.any { it.isLetter() }) return true
        val headers = listOf(
            "experience", "education", "skills", "summary", "profile",
            "projects", "publications", "certifications", "awards",
            "work history", "employment", "technical skills",
        )
        return headers.any { h -> t == h || t.startsWith("$h:") }
    }

    private fun looksLikeBullet(lower: String): Boolean {
        val t = lower.trimStart()
        return t.startsWith("•") || t.startsWith("-") || t.startsWith("*") ||
            Regex("^\\d+\\.\\s").containsMatchIn(t)
    }

    private fun looksLikeNameLine(lower: String): Boolean {
        val words = lower.split(Regex("\\s+")).filter { it.any { c -> c.isLetter() } }
        return words.size in 2..4 && lower.length < 60 && !lower.contains('@')
    }

    private fun containsYearRange(lower: String): Boolean =
        Regex("(19|20)\\d{2}").containsMatchIn(lower)

    private fun genericBoilerplate(lower: String): Boolean {
        val phrases = listOf(
            "references available",
            "upon request",
            "i am a highly motivated",
            "team player",
            "detail-oriented",
            "proven track record of success",
        )
        return phrases.any { lower.contains(it) }
    }
}
