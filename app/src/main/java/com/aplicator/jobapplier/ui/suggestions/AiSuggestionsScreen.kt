package com.aplicator.jobapplier.ui.suggestions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aplicator.jobapplier.data.remote.ai.AiSuggestionsResponse
import com.aplicator.jobapplier.ui.components.CopyCard
import com.aplicator.jobapplier.ui.components.EmptyState
import com.aplicator.jobapplier.ui.components.PremiumLoadingIndicator
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.theme.AccentAmber
import com.aplicator.jobapplier.ui.theme.AccentCyan
import com.aplicator.jobapplier.ui.theme.AccentTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuggestionsScreen(viewModel: AiSuggestionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI coach", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            when (val currentState = state) {
                is SuggestionsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumLoadingIndicator(message = "Analyzing your profile...")
                }
                is SuggestionsState.Error -> EmptyState(
                    title = "AI suggestions unavailable",
                    message = currentState.message,
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.fillMaxSize(),
                )
                is SuggestionsState.Empty -> EmptyState(
                    title = "Complete your profile",
                    message = "Add profile details to unlock tailored headlines, summaries, gaps, and tips.",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.fillMaxSize(),
                )
                is SuggestionsState.Success -> SuggestionsContent(currentState.suggestions, snackbarHostState)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionsContent(
    suggestions: AiSuggestionsResponse,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        scope.launch { snackbarHostState.showSnackbar("Copied: $label") }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SaasCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                Text("Profile intelligence", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "Use these suggestions to improve your profile and application copy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (suggestions.headlineSuggestions.isNotEmpty()) {
            item {
                SuggestionSection("Headline ideas", AccentTeal, Icons.AutoMirrored.Filled.TrendingUp) {
                    suggestions.headlineSuggestions.forEach { headline ->
                        CopyCard(label = "Headline", value = headline, accent = AccentTeal, onClick = { copy("Headline", headline) })
                    }
                }
            }
        }
        if (suggestions.summaryRewrites.isNotEmpty()) {
            item {
                SuggestionSection("Summary rewrites", AccentCyan, Icons.Default.AutoAwesome) {
                    suggestions.summaryRewrites.forEachIndexed { index, summary ->
                        CopyCard(label = "Option ${index + 1}", value = summary, accent = AccentCyan, onClick = { copy("Summary option ${index + 1}", summary) })
                    }
                }
            }
        }
        if (suggestions.skillGaps.isNotEmpty()) {
            item {
                SuggestionSection("Skill gaps", AccentAmber, Icons.Default.Lightbulb) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.skillGaps.forEach { gap ->
                            val color = when (gap.priority) {
                                "high" -> MaterialTheme.colorScheme.error
                                "medium" -> AccentAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                color = color.copy(alpha = 0.10f),
                                contentColor = color,
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(gap.skill, fontWeight = FontWeight.Bold)
                                    Text(gap.reason, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (suggestions.generalTips.isNotEmpty()) {
            item {
                SuggestionSection("Tips", MaterialTheme.colorScheme.primary, Icons.Default.Lightbulb) {
                    suggestions.generalTips.forEach { tip ->
                        Text("• $tip", modifier = Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSection(
    title: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    SaasCard(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(
                title,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
