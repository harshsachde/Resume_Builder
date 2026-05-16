package com.resumebuilder.tailor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParagraphScorerTest {

    @Test
    fun keywordSet_collectsRoleCompanyAndDescription() {
        val ctx = JobContext(
            jobTitle = "Senior Android Engineer",
            companyName = "Acme Corp",
            jobDescription = "Kotlin Jetpack Compose",
            targetPages = 1,
        )
        val k = ParagraphScorer.keywordSet(ctx)
        assertTrue(k.contains("kotlin"))
        assertTrue(k.contains("android"))
        assertTrue(k.contains("acme"))
    }

    @Test
    fun score_boostsKeywordMatches() {
        val keywords = setOf("kotlin", "compose")
        val s = ParagraphScorer.score("Built Kotlin UI with Jetpack Compose", keywords)
        assertTrue(s > 20)
    }
}
