package com.resumebuilder.tailor

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumebuilder.tailor.ui.theme.ResumeTailorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResumeTailorTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.state.collectAsState()
                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                LaunchedEffect(state.message) {
                    val m = state.message ?: return@LaunchedEffect
                    snackbar.showSnackbar(m)
                    vm.clearMessage()
                }

                val pickResume = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent(),
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val label = queryDisplayName(context.contentResolver, uri) ?: uri.toString()
                    val mime = context.contentResolver.getType(uri) ?: ""
                    val isPdf = mime == "application/pdf" ||
                        mime == "application/x-pdf" ||
                        label.lowercase().endsWith(".pdf")
                    vm.setResumeUri(uri, label, isPdf)
                }

                val pickTemplate = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent(),
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val label = queryDisplayName(context.contentResolver, uri) ?: uri.toString()
                    vm.setTemplateUri(uri, label)
                }

                val saveDoc = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    ),
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val bytes = state.lastOutputBytes ?: return@rememberLauncherForActivityResult
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outs ->
                                outs.write(bytes)
                            }
                        }
                        snackbar.showSnackbar("Saved tailored resume.")
                        vm.consumeOutput()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Resume Tailor") },
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { inner ->
                    Column(
                        modifier = Modifier
                            .padding(inner)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Keeps your Word template (fonts, colors, spacing, styles). " +
                                "Removes lower‑relevance blocks to hit your page target using the role, " +
                                "company, and description you provide. Optional strict ATS mode strips " +
                                "inline drawings/clip art while keeping text.",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Button(
                            onClick = { pickResume.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Select resume (.docx or .pdf)")
                        }
                        Text(state.resumeLabel, style = MaterialTheme.typography.bodySmall)

                        Button(
                            onClick = { pickTemplate.launch(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            ) },
                            enabled = state.isPdfResume,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Select Word template (.docx) for PDF styling")
                        }
                        Text(state.templateLabel, style = MaterialTheme.typography.bodySmall)

                        OutlinedTextField(
                            value = state.jobTitle,
                            onValueChange = vm::setJobTitle,
                            label = { Text("Job title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.companyName,
                            onValueChange = vm::setCompanyName,
                            label = { Text("Company") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.jobDescription,
                            onValueChange = vm::setJobDescription,
                            label = { Text("Job description / keywords") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                        )
                        OutlinedTextField(
                            value = state.targetPages,
                            onValueChange = vm::setTargetPages,
                            label = { Text("Target pages (1–5)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = state.strictAts,
                                    onCheckedChange = vm::setStrictAts,
                                )
                                Text(
                                    "Strict ATS mode (remove drawings/embedded pictures; keep text)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Button(
                            onClick = { vm.tailor(context) },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.busy) "Working…" else "Tailor resume")
                        }

                        Button(
                            onClick = {
                                saveDoc.launch("tailored-resume.docx")
                            },
                            enabled = state.lastOutputBytes != null && !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save tailored .docx")
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(cr: android.content.ContentResolver, uri: Uri): String? {
    cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return c.getString(idx)
        }
    }
    return null
}
