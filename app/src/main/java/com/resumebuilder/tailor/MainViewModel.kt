package com.resumebuilder.tailor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class TailorUiState(
    val resumeLabel: String = "No resume selected",
    val templateLabel: String = "No Word template (required for PDF)",
    val jobTitle: String = "",
    val companyName: String = "",
    val jobDescription: String = "",
    val targetPages: String = "1",
    val strictAts: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val lastOutputBytes: ByteArray? = null,
    val resumeUri: Uri? = null,
    val templateUri: Uri? = null,
    val isPdfResume: Boolean = false,
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(TailorUiState())
    val state: StateFlow<TailorUiState> = _state.asStateFlow()

    fun setResumeUri(uri: Uri?, label: String, isPdf: Boolean) {
        _state.value = _state.value.copy(
            resumeUri = uri,
            resumeLabel = label,
            isPdfResume = isPdf,
            message = null,
        )
    }

    fun setTemplateUri(uri: Uri?, label: String) {
        _state.value = _state.value.copy(
            templateUri = uri,
            templateLabel = label,
            message = null,
        )
    }

    fun setJobTitle(v: String) {
        _state.value = _state.value.copy(jobTitle = v)
    }

    fun setCompanyName(v: String) {
        _state.value = _state.value.copy(companyName = v)
    }

    fun setJobDescription(v: String) {
        _state.value = _state.value.copy(jobDescription = v)
    }

    fun setTargetPages(v: String) {
        _state.value = _state.value.copy(targetPages = v)
    }

    fun setStrictAts(v: Boolean) {
        _state.value = _state.value.copy(strictAts = v)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun consumeOutput() {
        _state.value = _state.value.copy(lastOutputBytes = null)
    }

    fun tailor(context: Context) {
        val s = _state.value
        val resumeUri = s.resumeUri
        if (resumeUri == null) {
            _state.value = s.copy(message = "Select a resume file first.")
            return
        }
        if (s.isPdfResume && s.templateUri == null) {
            _state.value = s.copy(message = "PDF resumes need a Word (.docx) template for fonts, margins, and theme.")
            return
        }
        val pages = s.targetPages.toIntOrNull()?.coerceIn(1, 5) ?: 1
        val job = JobContext(
            jobTitle = s.jobTitle.trim(),
            companyName = s.companyName.trim(),
            jobDescription = s.jobDescription.trim(),
            targetPages = pages,
        )

        viewModelScope.launch {
            _state.value = s.copy(busy = true, message = null)
            try {
                val out = withContext(Dispatchers.IO) {
                    val cr = context.contentResolver
                    if (s.isPdfResume) {
                        val pdfBytes = readUriBytes(cr, resumeUri)
                        val paras = pdfBytes.inputStream().use { ins ->
                            PdfTextExtractor.extractParagraphs(ins, context)
                        }
                        val indexed = paras.mapIndexed { idx, text -> idx to text }
                        val templateBytes = readUriBytes(cr, s.templateUri!!)
                        DocxTailor.tailorPdfWithTemplate(indexed, templateBytes, job, s.strictAts)
                    } else {
                        val docxBytes = readUriBytes(cr, resumeUri)
                        DocxTailor.tailorDocx(docxBytes, job, s.strictAts)
                    }
                }
                _state.value = _state.value.copy(
                    busy = false,
                    lastOutputBytes = out,
                    message = "Tailored document ready to save.",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    busy = false,
                    message = e.message ?: "Could not tailor this file.",
                )
            }
        }
    }

    private fun readUriBytes(cr: ContentResolver, uri: Uri): ByteArray {
        cr.openInputStream(uri)?.use { ins ->
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                bos.write(buf, 0, n)
            }
            return bos.toByteArray()
        } ?: error("Unable to open $uri")
    }
}
