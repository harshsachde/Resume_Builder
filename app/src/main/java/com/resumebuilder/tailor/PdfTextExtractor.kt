package com.resumebuilder.tailor

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

object PdfTextExtractor {

    fun extractParagraphs(inputStream: InputStream, context: android.content.Context): List<String> {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(inputStream).use { doc ->
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }
            val raw = stripper.getText(doc)
            return splitIntoParagraphs(raw)
        }
    }

    internal fun splitIntoParagraphs(raw: String): List<String> {
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
        val blocks = normalized.split(Regex("\n\\s*\n"))
            .map { it.trim().replace(Regex("\n+"), " ") }
            .filter { it.isNotEmpty() }
        if (blocks.size >= 2) return blocks
        return normalized.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
