package com.aplicator.jobapplier.ui.job

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(
    viewModel: JobViewModel,
    onBack: () -> Unit,
    onJobAnalyzed: (String) -> Unit,
) {
    val addJobState by viewModel.addJobState.collectAsState()
    val fetchState by viewModel.fetchUrlState.collectAsState()
    var companyName by rememberSaveable { mutableStateOf("") }
    var roleTitle by rememberSaveable { mutableStateOf("") }
    var rawText by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.consumeSharedText()?.let { shared ->
            rawText = shared
        }
    }

    LaunchedEffect(addJobState.analyzedJobId) {
        addJobState.analyzedJobId?.let {
            viewModel.clearAddJobState()
            onJobAnalyzed(it)
        }
    }

    LaunchedEffect(addJobState.error) {
        addJobState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(fetchState) {
        val state = fetchState
        if (state is FetchUrlState.Success) {
            state.title?.let { roleTitle = it }
            state.company?.let { companyName = it }
            state.description?.let { rawText = it }
            viewModel.clearFetchState()
        } else if (state is FetchUrlState.Failed) {
            snackbarHostState.showSnackbar("Could not auto-fetch. Please paste the job description below.")
            viewModel.clearFetchState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Application") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Paste the job description or enter a LinkedIn URL to auto-fetch",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                label = { Text("Job URL (LinkedIn or other)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (sourceUrl.isNotBlank() && fetchState !is FetchUrlState.Loading) {
                        IconButton(onClick = { viewModel.fetchJobFromUrl(sourceUrl.trim()) }) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = "Auto-fetch",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )

            AnimatedVisibility(visible = sourceUrl.isNotBlank() && fetchState !is FetchUrlState.Loading) {
                TextButton(
                    onClick = { viewModel.fetchJobFromUrl(sourceUrl.trim()) },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-fetch job details")
                }
            }

            AnimatedVisibility(visible = fetchState is FetchUrlState.Loading) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Fetching job details...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = roleTitle,
                onValueChange = { roleTitle = it },
                label = { Text("Job Title / Role") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Job Description",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let { rawText = it }
                    },
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
                maxLines = 20,
                placeholder = { Text("Paste the full job description here...") },
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.analyzeJob(companyName.trim(), roleTitle.trim(), rawText.trim(), sourceUrl.trim().ifBlank { null })
                },
                enabled = !addJobState.isAnalyzing && companyName.isNotBlank() && roleTitle.isNotBlank() && rawText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (addJobState.isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Analyze & Save")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
