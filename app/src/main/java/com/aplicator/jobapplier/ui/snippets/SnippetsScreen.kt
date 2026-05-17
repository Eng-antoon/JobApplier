package com.aplicator.jobapplier.ui.snippets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.domain.model.Certification
import com.aplicator.jobapplier.domain.model.Education
import com.aplicator.jobapplier.domain.model.Language
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import com.aplicator.jobapplier.service.BubbleOverlayService
import com.aplicator.jobapplier.ui.components.CopyCard
import com.aplicator.jobapplier.ui.components.EmptyState
import com.aplicator.jobapplier.ui.components.OverlayPermissionDialog
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.SaasSearchField
import com.aplicator.jobapplier.ui.components.SelectableSaasChip
import com.aplicator.jobapplier.ui.profile.ProfileUiState
import com.aplicator.jobapplier.ui.profile.ProfileViewModel
import com.aplicator.jobapplier.ui.theme.AccentAmber
import com.aplicator.jobapplier.ui.theme.AccentCyan
import com.aplicator.jobapplier.ui.theme.AccentIndigo
import com.aplicator.jobapplier.ui.theme.AccentTeal
import kotlinx.coroutines.launch

data class SnippetItem(
    val category: String,
    val label: String,
    val value: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SnippetsScreen(profileViewModel: ProfileViewModel) {
    val state by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val snippets = remember(state.profile, state.skills, state.experiences, state.educationList, state.languages, state.certifications) {
        buildSnippets(state)
    }
    val categories = listOf("All") + snippets.map { it.category }.distinct()
    val visibleSnippets = snippets.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
            (searchQuery.isBlank() ||
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true))
    }

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        scope.launch { snackbarHostState.showSnackbar("Copied: $label") }
    }

    if (showPermissionDialog) {
        OverlayPermissionDialog(onDismiss = { showPermissionDialog = false }, onGranted = { showPermissionDialog = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Quick copy", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        context.startForegroundService(Intent(context, BubbleOverlayService::class.java))
                        scope.launch { snackbarHostState.showSnackbar("Bubble launched") }
                    } else {
                        showPermissionDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.BubbleChart, contentDescription = "Launch Bubble", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            if (snippets.isEmpty()) {
                EmptyState(
                    title = "No snippets yet",
                    message = "Add profile data to create one-tap copy cards for forms and applications.",
                    icon = Icons.Default.Style,
                    modifier = Modifier.fillMaxSize(),
                )
                return@SaasScreenBackground
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SaasCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("Application snippets", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "Search, copy, or launch the floating bubble for other apps.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    SaasSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search snippets",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            SelectableSaasChip(
                                label = category.replaceFirstChar { it.uppercase() },
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                            )
                        }
                    }
                }

                val grouped = visibleSnippets.groupBy { it.category }
                grouped.forEach { (category, items) ->
                    item(key = "header_$category") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                copy(
                                    "All ${category.replaceFirstChar { it.uppercase() }}",
                                    items.joinToString("\n") { "${it.label}: ${it.value}" },
                                )
                            }) {
                                Icon(Icons.Default.CopyAll, contentDescription = "Copy all")
                            }
                        }
                    }
                    items(items, key = { "${it.category}_${it.label}_${it.value.hashCode()}" }) { snippet ->
                        CopyCard(
                            label = snippet.label,
                            value = snippet.value,
                            accent = categoryColor(snippet.category),
                            onClick = { copy(snippet.label, snippet.value) },
                        )
                    }
                }
                item { Spacer(Modifier.height(84.dp)) }
            }
        }
    }
}

private fun categoryColor(category: String): Color = when {
    category == "personal" -> AccentTeal
    category == "skills" -> AccentCyan
    category == "education" -> AccentIndigo
    category == "languages" -> AccentAmber
    category == "certifications" -> AccentIndigo
    category == "summary" -> AccentTeal
    else -> AccentCyan
}

private fun buildSnippets(state: ProfileUiState): List<SnippetItem> {
    val profile = state.profile ?: return emptyList()
    return buildSnippets(
        profile, state.skills, state.experiences,
        state.educationList, state.certifications, state.languages,
    )
}

fun buildSnippets(
    profile: Profile,
    skills: List<Skill>,
    experiences: List<WorkExperience>,
    educationList: List<Education>,
    certifications: List<Certification>,
    languages: List<Language>,
): List<SnippetItem> {
    val snippets = mutableListOf<SnippetItem>()

    if (profile.fullName.isNotBlank()) snippets.add(SnippetItem("personal", "Full Name", profile.fullName))
    profile.email?.let { snippets.add(SnippetItem("personal", "Email", it)) }
    profile.phone?.let { snippets.add(SnippetItem("personal", "Phone", it)) }
    profile.location?.let { snippets.add(SnippetItem("personal", "Location", it)) }
    profile.linkedinUrl?.let { snippets.add(SnippetItem("personal", "LinkedIn", it)) }
    profile.desiredRole?.let { snippets.add(SnippetItem("personal", "Desired Role", it)) }
    profile.portfolioUrl?.let { snippets.add(SnippetItem("personal", "Portfolio", it)) }

    skills.forEach { skill ->
        val value = buildString {
            append(skill.name)
            skill.yearsExperience?.let { append(" - $it years") }
            skill.proficiency?.let { append(" ($it)") }
        }
        snippets.add(SnippetItem("skills", skill.name, value))
    }

    experiences.forEach { exp ->
        val groupName = "${exp.title} at ${exp.company}"
        val dates = "${exp.startDate ?: "N/A"} - ${exp.endDate ?: "Present"}"
        snippets.add(SnippetItem(groupName, "Role & Period", "$groupName ($dates)"))
        exp.description?.takeIf { it.isNotBlank() }?.let { snippets.add(SnippetItem(groupName, "Description", it)) }
        if (exp.achievements.isNotEmpty()) {
            snippets.add(SnippetItem(groupName, "Achievements", exp.achievements.joinToString("\n• ", prefix = "• ")))
        }
    }

    educationList.forEach { edu ->
        snippets.add(SnippetItem("education", edu.degree, "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""} from ${edu.institution}"))
    }
    languages.forEach { lang ->
        snippets.add(SnippetItem("languages", lang.name, "${lang.name}${lang.proficiency?.let { " ($it)" } ?: ""}"))
    }
    certifications.forEach { cert ->
        snippets.add(SnippetItem("certifications", cert.name, "${cert.name} - ${cert.issuingOrg}${cert.issueDate?.let { " ($it)" } ?: ""}"))
    }
    profile.summary?.let { snippets.add(SnippetItem("summary", "Professional Summary", it)) }

    return snippets
}
