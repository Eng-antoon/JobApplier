package com.aplicator.jobapplier.ui.snippets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.R
import com.aplicator.jobapplier.service.BubbleOverlayService
import com.aplicator.jobapplier.ui.components.OverlayPermissionDialog
import com.aplicator.jobapplier.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch

data class SnippetItem(
    val category: String,
    val label: String,
    val value: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SnippetsScreen(
    profileViewModel: ProfileViewModel,
) {
    val state by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val snippets = remember(state.profile, state.skills, state.experiences, state.educationList, state.languages) {
        buildSnippets(state)
    }

    val groupedSnippets = snippets.groupBy { it.category }

    if (showPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onGranted = { showPermissionDialog = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Quick Copy") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        val intent = Intent(context, BubbleOverlayService::class.java)
                        context.startForegroundService(intent)
                        scope.launch { snackbarHostState.showSnackbar("Bubble launched!") }
                    } else {
                        showPermissionDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.BubbleChart, contentDescription = "Launch Bubble")
            }
        },
    ) { padding ->
        if (snippets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.il_empty_snippets),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "No snippets yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add profile data to generate quick-copy snippets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Tap any chip to copy its value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            groupedSnippets.forEach { (category, items) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    if (category == "skills") {
                        IconButton(onClick = {
                            val allSkills = items.joinToString(", ") { it.label }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("All Skills", allSkills))
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch { snackbarHostState.showSnackbar("Copied all ${items.size} skills") }
                        }) {
                            Icon(Icons.Default.CopyAll, contentDescription = "Copy All Skills", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items.forEach { snippet ->
                        SuggestionChip(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(snippet.label, snippet.value))
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                scope.launch { snackbarHostState.showSnackbar("Copied: ${snippet.label}") }
                            },
                            label = { Text(snippet.label) },
                            icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun buildSnippets(state: com.aplicator.jobapplier.ui.profile.ProfileUiState): List<SnippetItem> {
    val snippets = mutableListOf<SnippetItem>()
    val profile = state.profile ?: return snippets

    if (profile.fullName.isNotBlank()) snippets.add(SnippetItem("personal", "Full Name", profile.fullName))
    profile.email?.let { snippets.add(SnippetItem("personal", "Email", it)) }
    profile.phone?.let { snippets.add(SnippetItem("personal", "Phone", it)) }
    profile.location?.let { snippets.add(SnippetItem("personal", "Location", it)) }
    profile.linkedinUrl?.let { snippets.add(SnippetItem("personal", "LinkedIn", it)) }
    profile.desiredRole?.let { snippets.add(SnippetItem("personal", "Desired Role", it)) }

    state.skills.forEach { skill ->
        val value = buildString {
            append(skill.name)
            skill.yearsExperience?.let { append(" - $it years") }
            skill.proficiency?.let { append(" ($it)") }
        }
        snippets.add(SnippetItem("skills", skill.name, value))
    }

    state.experiences.forEach { exp ->
        val groupName = "${exp.title} at ${exp.company}"
        val dates = "${exp.startDate ?: "N/A"} - ${exp.endDate ?: "Present"}"
        snippets.add(SnippetItem(groupName, "Role & Period", "$groupName ($dates)"))
        exp.description?.let { desc ->
            if (desc.isNotBlank()) {
                snippets.add(SnippetItem(groupName, "Description", desc))
            }
        }
        if (exp.achievements.isNotEmpty()) {
            snippets.add(SnippetItem(groupName, "Achievements", exp.achievements.joinToString("\n• ", prefix = "• ")))
        }
    }

    state.educationList.forEach { edu ->
        snippets.add(SnippetItem("education", edu.degree, "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""} from ${edu.institution}"))
    }

    state.languages.forEach { lang ->
        snippets.add(SnippetItem("languages", lang.name, "${lang.name}${lang.proficiency?.let { " ($it)" } ?: ""}"))
    }

    profile.portfolioUrl?.let { snippets.add(SnippetItem("personal", "Portfolio", it)) }

    if (profile.desiredSalaryMin != null) {
        val salary = "${profile.desiredSalaryMin} - ${profile.desiredSalaryMax ?: "N/A"} ${profile.salaryCurrency}"
        snippets.add(SnippetItem("personal", "Salary Expectation", salary))
    }

    state.certifications.forEach { cert ->
        val value = buildString {
            append(cert.name)
            if (cert.issuingOrg.isNotBlank()) append(" - ${cert.issuingOrg}")
            cert.issueDate?.let { append(" ($it)") }
        }
        snippets.add(SnippetItem("certifications", cert.name, value))
    }

    profile.summary?.let { snippets.add(SnippetItem("summary", "Professional Summary", it)) }

    return snippets
}
