package com.aplicator.jobapplier.ui.job

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.analytics.NoOpAnalyticsTracker
import com.aplicator.jobapplier.ui.components.CompanyAvatar
import com.aplicator.jobapplier.ui.components.EmptyState
import com.aplicator.jobapplier.ui.components.MatchScoreRing
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.ShimmerJobCard
import com.aplicator.jobapplier.ui.components.StatusPill
import com.aplicator.jobapplier.ui.components.scoreColor
import com.aplicator.jobapplier.ui.theme.AccentCyan
import com.aplicator.jobapplier.ui.theme.AccentIndigo
import com.aplicator.jobapplier.ui.theme.MatchHigh
import com.aplicator.jobapplier.ui.theme.MatchLow
import com.aplicator.jobapplier.ui.theme.MatchMedium
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobAnalysisScreen(
    jobId: String,
    viewModel: JobViewModel,
    onBack: () -> Unit,
    analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    val detailState by viewModel.jobDetailState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobId) { viewModel.loadJobDetail(jobId) }
    LaunchedEffect(detailState.job?.id) {
        detailState.job?.let {
            analyticsTracker.track(
                AnalyticsEvent(AnalyticsEvents.SCREEN_VIEWED, mapOf("screen" to "JobAnalysis")),
            )
        }
    }

    fun openUrl(url: String) {
        val normalized = url.trim().let { if (it.startsWith("http", ignoreCase = true)) it else "https://$it" }
        val opened = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        if (!opened) scope.launch { snackbarHostState.showSnackbar("Could not open link") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Full job insights",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
            val match = detailState.matchResult
            val requirements = job.requirementsExtracted
            val hasAnalysis = job.matchScore != null || match != null || requirements.isNotEmpty()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                SaasCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyAvatar(job.companyName)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                job.companyName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                job.roleTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusPill(job.status)
                        }
                    }
                }

                if (!hasAnalysis) {
                    SaasCard(Modifier.fillMaxWidth()) {
                        EmptyState(
                            title = "No analysis yet",
                            message = "Analyze this job from the application screen to see match details and requirements.",
                            icon = Icons.Default.Insights,
                        )
                    }
                    Spacer(Modifier.height(80.dp))
                    return@Column
                }

                // Score hero
                job.matchScore?.let { score ->
                    val color = scoreColor(score)
                    SaasCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MatchScoreRing(score, size = 92.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "$score% match",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = color,
                            )
                            Text(
                                matchLabel(score),
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                            )
                            if (match != null) {
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MatchStat("Matched", match.matched.size, MatchHigh, Modifier.weight(1f))
                                    MatchStat("Partial", match.partial.size, MatchMedium, Modifier.weight(1f))
                                    MatchStat("Gaps", match.gaps.size, MatchLow, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Source link
                job.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    SaasCard(modifier = Modifier.fillMaxWidth(), onClick = { openUrl(url) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp),
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    sourceUrlLabel(url),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Requirements
                if (requirements.isNotEmpty()) {
                    SaasCard(Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Extracted requirements",
                            subtitle = "${requirements.size} found",
                            icon = Icons.AutoMirrored.Filled.List,
                            tint = AccentIndigo,
                        )
                        Spacer(Modifier.height(12.dp))
                        SkillSection(requirements, AccentIndigo, Icons.AutoMirrored.Filled.List)
                    }
                }

                // Fit breakdown
                if (match != null && (match.matched.isNotEmpty() || match.partial.isNotEmpty() || match.gaps.isNotEmpty())) {
                    SaasCard(Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Fit breakdown",
                            subtitle = "How your profile maps to this role",
                            icon = Icons.Default.Insights,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        SkillSection(match.matched, MatchHigh, Icons.Default.CheckCircle, label = "Matched")
                        SkillSection(match.partial, MatchMedium, Icons.Default.HorizontalRule, label = "Partial")
                        SkillSection(match.gaps, MatchLow, Icons.Default.AutoAwesome, label = "Gaps")
                    }
                }

                // Suggestions
                if (!match?.suggestions.isNullOrEmpty()) {
                    SaasCard(Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Suggestions",
                            subtitle = "Ideas to strengthen your application",
                            icon = Icons.Default.Lightbulb,
                            tint = AccentCyan,
                        )
                        Spacer(Modifier.height(8.dp))
                        match.suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                )
                                Text(
                                    suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MatchStat(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillSection(items: List<String>, color: Color, icon: ImageVector, label: String? = null) {
    if (items.isEmpty()) return
    if (label != null) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = color.copy(alpha = 0.10f),
                contentColor = color,
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        item,
                        modifier = Modifier.padding(start = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

private fun sourceUrlLabel(url: String): String =
    if (url.contains("linkedin.com", ignoreCase = true)) "View on LinkedIn" else "View job posting"

private fun matchLabel(score: Int): String = when {
    score >= 70 -> "Strong match"
    score >= 40 -> "Moderate match"
    else -> "Needs work"
}
