package com.aplicator.jobapplier.ui.job

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aplicator.jobapplier.ui.webextract.WebJobExtractorContract
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.ui.components.PremiumLoadingIndicator
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasPrimaryButton
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.SaasSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(
    viewModel: JobViewModel,
    onBack: () -> Unit,
    onJobAnalyzed: (String) -> Unit,
) {
    val addJobState by viewModel.addJobState.collectAsState()
    val fetchState by viewModel.fetchUrlState.collectAsState()
    val linkedInUrl by viewModel.linkedInUrlToExtract.collectAsState()
    var companyName by rememberSaveable { mutableStateOf("") }
    var roleTitle by rememberSaveable { mutableStateOf("") }
    var rawText by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val webExtractLauncher = rememberLauncherForActivityResult(
        contract = WebJobExtractorContract(),
    ) { result -> viewModel.onWebExtractResult(result) }

    LaunchedEffect(linkedInUrl) {
        linkedInUrl?.let { url ->
            webExtractLauncher.launch(url)
            viewModel.clearLinkedInExtractRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.consumeSharedText()?.let { rawText = it }
    }

    LaunchedEffect(addJobState.analyzedJobId) {
        addJobState.analyzedJobId?.let {
            viewModel.clearAddJobState()
            onJobAnalyzed(it)
        }
    }

    LaunchedEffect(addJobState.error) {
        addJobState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(fetchState) {
        val state = fetchState
        if (state is FetchUrlState.Success) {
            state.title?.let { roleTitle = it }
            state.company?.let { companyName = it }
            state.description?.let { rawText = it }
            viewModel.clearFetchState()
        } else if (state is FetchUrlState.Failed) {
            val message = when (state.reason) {
                "blocked_by_auth" -> "This job post requires sign-in. Copy and paste the job description below."
                "no_job_content" -> "No job posting found at this URL. Please paste the job description below."
                "extraction_failed" -> "Could not extract from LinkedIn. Try copying and pasting the description."
                else -> "Could not auto-fetch. Please paste the job description below."
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearFetchState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New application", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SaasCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Add role details",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Paste a job post or fetch from a URL to score it against your profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SaasCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Source", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("Job URL") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (sourceUrl.isNotBlank() && fetchState !is FetchUrlState.Loading) {
                                IconButton(onClick = { viewModel.fetchJobFromUrl(sourceUrl.trim()) }) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Auto-fetch")
                                }
                            }
                        },
                    )
                    AnimatedVisibility(visible = fetchState is FetchUrlState.Loading) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                "Fetching job details...",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                SaasCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Role details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = roleTitle,
                        onValueChange = { roleTitle = it },
                        label = { Text("Job title / role") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                SaasCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Job description", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        SaasSecondaryButton(
                            onClick = { clipboardManager.getText()?.text?.let { rawText = it } },
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Paste")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 10,
                        maxLines = 18,
                        placeholder = { Text("Paste the full job description here...") },
                    )
                    Text(
                        "${rawText.length} characters",
                        modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SaasPrimaryButton(
                    onClick = {
                        viewModel.analyzeJob(
                            companyName.trim(),
                            roleTitle.trim(),
                            rawText.trim(),
                            sourceUrl.trim().ifBlank { null },
                        )
                    },
                    enabled = !addJobState.isAnalyzing &&
                        companyName.isNotBlank() &&
                        roleTitle.isNotBlank() &&
                        rawText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (addJobState.isAnalyzing) {
                        PremiumLoadingIndicator(message = null, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Analyze & save", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
