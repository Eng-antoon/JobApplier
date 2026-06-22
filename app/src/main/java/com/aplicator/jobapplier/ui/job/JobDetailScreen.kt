package com.aplicator.jobapplier.ui.job

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Title
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.analytics.NoOpAnalyticsTracker
import com.aplicator.jobapplier.data.export.ExportFormat
import com.aplicator.jobapplier.data.export.GeneratedContentExport
import com.aplicator.jobapplier.ui.components.CompanyAvatar
import com.aplicator.jobapplier.ui.components.GeneratedContentExportDialog
import com.aplicator.jobapplier.ui.components.MatchScoreRing
import com.aplicator.jobapplier.ui.components.PremiumLoadingIndicator
import com.aplicator.jobapplier.ui.components.QuotaExceededDialog
import com.aplicator.jobapplier.ui.components.QuotaRequestSuccessDialog
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasPrimaryButton
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.SaasSecondaryButton
import com.aplicator.jobapplier.ui.components.SelectableSaasChip
import com.aplicator.jobapplier.ui.components.ShimmerJobCard
import com.aplicator.jobapplier.ui.components.StatusPill
import com.aplicator.jobapplier.ui.components.defaultAiFillActions
import com.aplicator.jobapplier.ui.components.generatedContentLabel
import com.aplicator.jobapplier.ui.components.scoreColor
import com.aplicator.jobapplier.ui.theme.AccentCyan
import com.mixpanel.android.sessionreplay.extensions.mpReplaySensitive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ContentExportTarget(
    val label: String,
    val content: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    viewModel: JobViewModel,
    onBack: () -> Unit,
    onFullInsights: () -> Unit = {},
    analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    val detailState by viewModel.jobDetailState.collectAsState()
    val generateState by viewModel.generateState.collectAsState()
    val quotaExceeded by viewModel.quotaExceeded.collectAsState()
    val quotaRequestPending by viewModel.quotaRequestPending.collectAsState()
    val quotaRequestSuccess by viewModel.quotaRequestSuccess.collectAsState()
    val quotaRequestSuccessType by viewModel.quotaRequestSuccessType.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var customQuestion by rememberSaveable { mutableStateOf("") }
    var selectedTone by rememberSaveable { mutableStateOf("professional") }
    var exportTarget by remember { mutableStateOf<ContentExportTarget?>(null) }

    quotaExceeded?.let { quota ->
        QuotaExceededDialog(
            quotaInfo = quota,
            onRequestExtra = { viewModel.requestExtraQuota() },
            onDismiss = { viewModel.dismissQuotaDialog() },
            requestPending = quotaRequestPending,
        )
    }

    if (quotaRequestSuccess) {
        QuotaRequestSuccessDialog(
            quotaType = quotaRequestSuccessType,
            onDismiss = { viewModel.dismissQuotaRequestSuccess() },
        )
    }

    LaunchedEffect(jobId) {
        viewModel.loadJobDetail(jobId)
    }
    LaunchedEffect(detailState.job?.id) {
        detailState.job?.let { job ->
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.JOB_DETAIL_VIEWED,
                    mapOf(
                        "has_match_score" to (job.matchScore != null),
                        "status" to job.status,
                    ),
                ),
            )
        }
    }
    LaunchedEffect(generateState.error) {
        generateState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        analyticsTracker.track(
            AnalyticsEvent(
                AnalyticsEvents.GENERATED_CONTENT_COPIED,
                mapOf("surface" to "app", "content_type" to label),
            ),
        )
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        scope.launch { snackbarHostState.showSnackbar("Copied: $label") }
    }

    fun suggestedExportName(label: String): String {
        val job = detailState.job
        return GeneratedContentExport.suggestedFileName(
            contentTypeLabel = label,
            companyName = job?.companyName.orEmpty(),
            roleTitle = job?.roleTitle.orEmpty(),
        )
    }

    fun exportContent(target: ContentExportTarget, fileName: String, format: ExportFormat) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                GeneratedContentExport.saveToDownloads(context, fileName, target.content, format)
            }
            result
                .onSuccess { savedName ->
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.GENERATED_CONTENT_EXPORTED,
                            mapOf(
                                "surface" to "app",
                                "content_type" to target.label,
                                "format" to format.name.lowercase(),
                            ),
                        ),
                    )
                    snackbarHostState.showSnackbar("Saved to Downloads: $savedName")
                }
                .onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "Could not export answer") }
        }
    }

    exportTarget?.let { target ->
        GeneratedContentExportDialog(
            suggestedFileName = suggestedExportName(target.label),
            onDismiss = { exportTarget = null },
            onExport = { fileName, format ->
                exportTarget = null
                exportContent(target, fileName, format)
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(detailState.job?.roleTitle ?: "Application", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            if (detailState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) { ShimmerJobCard() }
                }
                return@SaasScreenBackground
            }

            val job = detailState.job ?: return@SaasScreenBackground
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyAvatar(job.companyName)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(job.companyName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(job.roleTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            StatusPill(job.status)
                        }
                        job.matchScore?.let { MatchScoreRing(it, size = 74.dp) }
                    }
                }

                SaasSecondaryButton(
                    onClick = onFullInsights,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(17.dp), tint = AccentCyan)
                    Text("Full job insights", modifier = Modifier.padding(start = 8.dp))
                }

                SaasCard(Modifier.fillMaxWidth()) {
                    Text("AI Fill Assistant", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Generate the text you need to fill applications automatically for this role.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("professional", "casual", "enthusiastic").forEach { tone ->
                            SelectableSaasChip(
                                label = tone.replaceFirstChar { it.uppercase() },
                                selected = selectedTone == tone,
                                onClick = { selectedTone = tone },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    defaultAiFillActions().forEach { action ->
                        val isPrimary = action.contentType == "cover_email" || action.contentType == "cover_letter"
                        val icon = when (action.contentType) {
                            "cover_email" -> Icons.Default.Email
                            "cover_letter" -> Icons.Default.Description
                            "headline" -> Icons.Default.Title
                            else -> Icons.Default.QuestionAnswer
                        }
                        if (isPrimary) {
                            SaasPrimaryButton(
                                onClick = { viewModel.generateContent(jobId, action.contentType, selectedTone, action.question) },
                                enabled = !generateState.isGenerating,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text(action.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            SaasSecondaryButton(
                                onClick = { viewModel.generateContent(jobId, action.contentType, selectedTone, action.question) },
                                enabled = !generateState.isGenerating,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = AccentCyan)
                                Text(action.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        label = { Text("Custom question") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    SaasSecondaryButton(
                        onClick = { viewModel.generateContent(jobId, "custom_question", selectedTone, customQuestion) },
                        enabled = !generateState.isGenerating && customQuestion.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Answer custom question")
                    }
                }

                AnimatedVisibility(
                    visible = generateState.isGenerating,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    SaasCard(Modifier.fillMaxWidth()) {
                        PremiumLoadingIndicator(message = "Generating tailored content...")
                    }
                }

                generateState.generatedText?.let { text ->
                    val label = generatedContentLabel(generateState.contentType ?: "generated_content")
                    SaasCard(
                        modifier = Modifier.fillMaxWidth().animateContentSize().mpReplaySensitive(true),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Generated content", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { exportTarget = ContentExportTarget(label, text) }) {
                                Icon(Icons.Default.Download, contentDescription = "Export")
                            }
                            IconButton(onClick = { copyToClipboard("Generated Content", text) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (detailState.generatedContent.isNotEmpty()) {
                    Text("Saved content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    detailState.generatedContent.forEach { content ->
                        val label = generatedContentLabel(content.contentType)
                        SaasCard(
                            modifier = Modifier.fillMaxWidth().mpReplaySensitive(true),
                            contentPadding = 12.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        content.content.take(220) + if (content.content.length > 220) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { exportTarget = ContentExportTarget(label, content.content) }) {
                                    Icon(Icons.Default.Download, contentDescription = "Export")
                                }
                                IconButton(onClick = { copyToClipboard(label, content.content) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

