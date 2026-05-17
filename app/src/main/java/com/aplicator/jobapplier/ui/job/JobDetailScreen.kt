package com.aplicator.jobapplier.ui.job

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.ui.components.ShimmerJobCard
import com.aplicator.jobapplier.ui.theme.MatchHigh
import com.aplicator.jobapplier.ui.theme.MatchLow
import com.aplicator.jobapplier.ui.theme.MatchMedium

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
    var selectedContentType by rememberSaveable { mutableStateOf<String?>(null) }
    var customQuestion by rememberSaveable { mutableStateOf("") }
    var selectedTone by rememberSaveable { mutableStateOf("professional") }

    LaunchedEffect(jobId) {
        viewModel.loadJobDetail(jobId)
    }

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
                title = { Text(detailState.job?.roleTitle ?: "Job Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (detailState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) { ShimmerJobCard() }
            }
            return@Scaffold
        }

        val job = detailState.job ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(job.companyName, style = MaterialTheme.typography.titleLarge)
            Text(job.roleTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // Match Score
            job.matchScore?.let { score ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            score >= 70 -> MatchHigh.copy(alpha = 0.1f)
                            score >= 40 -> MatchMedium.copy(alpha = 0.1f)
                            else -> MatchLow.copy(alpha = 0.1f)
                        }
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${score}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    score >= 70 -> MatchHigh
                                    score >= 40 -> MatchMedium
                                    else -> MatchLow
                                },
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Match Score", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Match Details
            detailState.matchResult?.let { match ->
                if (match.matched.isNotEmpty()) {
                    Text("Matched Skills", style = MaterialTheme.typography.titleMedium, color = MatchHigh)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.matched.forEach { item ->
                            AssistChip(onClick = {}, label = { Text(item) }, colors = AssistChipDefaults.assistChipColors(containerColor = MatchHigh.copy(alpha = 0.1f)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (match.gaps.isNotEmpty()) {
                    Text("Gaps", style = MaterialTheme.typography.titleMedium, color = MatchLow)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.gaps.forEach { item ->
                            AssistChip(onClick = {}, label = { Text(item) }, colors = AssistChipDefaults.assistChipColors(containerColor = MatchLow.copy(alpha = 0.1f)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (match.suggestions.isNotEmpty()) {
                    Text("Suggestions", style = MaterialTheme.typography.titleMedium)
                    match.suggestions.forEach { suggestion ->
                        Text("• $suggestion", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Generate Content Section
            Text("Generate Content", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            // Tone selector
            Text("Tone", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("professional", "casual", "enthusiastic").forEach { tone ->
                    AssistChip(
                        onClick = { selectedTone = tone },
                        label = { Text(tone.replaceFirstChar { it.uppercase() }) },
                        colors = if (selectedTone == tone)
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else AssistChipDefaults.assistChipColors(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Content type buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectedContentType = "cover_letter"
                        viewModel.generateContent(jobId, "cover_letter", selectedTone)
                    },
                    enabled = !generateState.isGenerating,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cover Letter", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = {
                        selectedContentType = "cover_email"
                        viewModel.generateContent(jobId, "cover_email", selectedTone)
                    },
                    enabled = !generateState.isGenerating,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cover Email", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Quick question buttons
            Text("Answer Questions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            listOf(
                "why_work_here" to "Why do you want to work here?",
                "strengths" to "What are your strengths?",
                "motivation" to "Tell me about yourself",
            ).forEach { (type, label) ->
                OutlinedButton(
                    onClick = {
                        selectedContentType = type
                        viewModel.generateContent(jobId, type, selectedTone, label)
                    },
                    enabled = !generateState.isGenerating,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }

            // Custom question
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                label = { Text("Custom Question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    selectedContentType = "custom_question"
                    viewModel.generateContent(jobId, "custom_question", selectedTone, customQuestion)
                },
                enabled = !generateState.isGenerating && customQuestion.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Answer Custom Question")
            }

            // Loading indicator
            AnimatedVisibility(
                visible = generateState.isGenerating,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Generated Content Display
            AnimatedVisibility(
                visible = generateState.generatedText != null,
                enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
            ) {
                generateState.generatedText?.let { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Generated Content", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { copyToClipboard("Generated Content", text) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Previously Generated Content
            if (detailState.generatedContent.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Saved Content", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                detailState.generatedContent.forEach { content ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    content.contentType.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                IconButton(onClick = { copyToClipboard(content.contentType, content.content) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                content.content.take(200) + if (content.content.length > 200) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
