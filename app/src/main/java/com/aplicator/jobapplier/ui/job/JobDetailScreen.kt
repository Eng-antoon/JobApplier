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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.ui.components.CompanyAvatar
import com.aplicator.jobapplier.ui.components.CopyCard
import com.aplicator.jobapplier.ui.components.MatchScoreRing
import com.aplicator.jobapplier.ui.components.PremiumLoadingIndicator
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasPrimaryButton
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.SaasSecondaryButton
import com.aplicator.jobapplier.ui.components.SelectableSaasChip
import com.aplicator.jobapplier.ui.components.ShimmerJobCard
import com.aplicator.jobapplier.ui.components.StatusPill
import com.aplicator.jobapplier.ui.components.scoreColor
import com.aplicator.jobapplier.ui.theme.MatchHigh
import com.aplicator.jobapplier.ui.theme.MatchLow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    viewModel: JobViewModel,
    onBack: () -> Unit,
) {
    val detailState by viewModel.jobDetailState.collectAsState()
    val generateState by viewModel.generateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    var customQuestion by rememberSaveable { mutableStateOf("") }
    var selectedTone by rememberSaveable { mutableStateOf("professional") }

    LaunchedEffect(jobId) { viewModel.loadJobDetail(jobId) }
    LaunchedEffect(generateState.error) {
        generateState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
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
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyAvatar(job.companyName)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(job.companyName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                            Text(job.roleTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f))
                            Spacer(Modifier.height(8.dp))
                            StatusPill(job.status)
                        }
                        job.matchScore?.let { MatchScoreRing(it, size = 74.dp) }
                    }
                }

                detailState.matchResult?.let { match ->
                    SaasCard(Modifier.fillMaxWidth()) {
                        Text("Fit breakdown", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        SkillSection("Matched", match.matched, MatchHigh, Icons.Default.CheckCircle)
                        SkillSection("Gaps", match.gaps, MatchLow, Icons.Default.AutoAwesome)
                        if (match.suggestions.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Suggestions", style = MaterialTheme.typography.labelLarge)
                            match.suggestions.forEach { suggestion ->
                                Text(
                                    "• $suggestion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }

                SaasCard(Modifier.fillMaxWidth()) {
                    Text("Generate tailored content", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose a tone and generate reusable copy for this role.",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SaasPrimaryButton(
                            onClick = { viewModel.generateContent(jobId, "cover_letter", selectedTone) },
                            enabled = !generateState.isGenerating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text("Letter", modifier = Modifier.padding(start = 6.dp))
                        }
                        SaasPrimaryButton(
                            onClick = { viewModel.generateContent(jobId, "cover_email", selectedTone) },
                            enabled = !generateState.isGenerating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text("Email", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "why_work_here" to "Why do you want to work here?",
                        "strengths" to "What are your strengths?",
                        "motivation" to "Tell me about yourself",
                    ).forEach { (type, label) ->
                        SaasSecondaryButton(
                            onClick = { viewModel.generateContent(jobId, type, selectedTone, label) },
                            enabled = !generateState.isGenerating,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        ) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text(label, modifier = Modifier.padding(start = 8.dp))
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
                    SaasCard(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Generated content", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                        CopyCard(
                            label = content.contentType.replace("_", " ").replaceFirstChar { it.uppercase() },
                            value = content.content.take(220) + if (content.content.length > 220) "..." else "",
                            accent = scoreColor(job.matchScore ?: 0),
                            onClick = { copyToClipboard(content.contentType, content.content) },
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillSection(title: String, items: List<String>, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
    ) {
        items.forEach { item ->
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                color = color.copy(alpha = 0.10f),
                contentColor = color,
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(item, modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
