package com.aplicator.jobapplier.ui.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasPrimaryButton
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.ShimmerProfileSection
import com.aplicator.jobapplier.ui.components.calculateProfileCompleteness
import com.aplicator.jobapplier.ui.theme.AccentTeal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToResumeImport: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    var fullName by rememberSaveable(state.profile) { mutableStateOf(state.profile?.fullName ?: "") }
    var phone by rememberSaveable(state.profile) { mutableStateOf(state.profile?.phone ?: "") }
    var location by rememberSaveable(state.profile) { mutableStateOf(state.profile?.location ?: "") }
    var linkedinUrl by rememberSaveable(state.profile) { mutableStateOf(state.profile?.linkedinUrl ?: "") }
    var summary by rememberSaveable(state.profile) { mutableStateOf(state.profile?.summary ?: "") }
    var desiredRole by rememberSaveable(state.profile) { mutableStateOf(state.profile?.desiredRole ?: "") }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Profile saved")
            isEditing = false
            viewModel.clearSaveSuccess()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isEditing) {
                        IconButton(
                            enabled = !state.isSaving,
                            onClick = {
                                state.profile?.let { profile ->
                                    viewModel.updateProfile(
                                        profile.copy(
                                            fullName = fullName,
                                            phone = phone.ifBlank { null },
                                            location = location.ifBlank { null },
                                            linkedinUrl = linkedinUrl.ifBlank { null },
                                            summary = summary.ifBlank { null },
                                            desiredRole = desiredRole.ifBlank { null },
                                        ),
                                    )
                                }
                            },
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
            )
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    repeat(3) { ShimmerProfileSection() }
                }
                return@SaasScreenBackground
            }

            val completeness = calculateProfileCompleteness(state)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileHeader(state.profile, completeness.percent)

                SaasCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onNavigateToResumeImport,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Import from resume", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Upload PDF or DOCX and let AI refresh your profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                SaasCard(Modifier.fillMaxWidth()) {
                    Text("Personal information", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (isEditing) {
                        EditableProfileFields(
                            fullName = fullName,
                            onFullName = { fullName = it },
                            phone = phone,
                            onPhone = { phone = it },
                            location = location,
                            onLocation = { location = it },
                            linkedinUrl = linkedinUrl,
                            onLinkedIn = { linkedinUrl = it },
                            desiredRole = desiredRole,
                            onDesiredRole = { desiredRole = it },
                            summary = summary,
                            onSummary = { summary = it },
                        )
                    } else {
                        ProfileField("Name", state.profile?.fullName)
                        ProfileField("Email", state.profile?.email)
                        ProfileField("Phone", state.profile?.phone)
                        ProfileField("Location", state.profile?.location)
                        ProfileField("LinkedIn", state.profile?.linkedinUrl)
                        ProfileField("Desired role", state.profile?.desiredRole)
                        ProfileField("Summary", state.profile?.summary)
                    }
                }

                SaasCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Skills", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showAddSkillDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Skill")
                        }
                    }
                    if (state.skills.isEmpty()) {
                        Text("No skills added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.skills.forEach { skill ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(skillLabel(skill)) },
                                    trailingIcon = if (isEditing) {
                                        {
                                            IconButton(onClick = { viewModel.deleteSkill(skill.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    } else null,
                                )
                            }
                        }
                    }
                }

                ProfileListSection(
                    title = "Work experience",
                    items = state.experiences.map { it.id to "${it.title} at ${it.company}" },
                    isEditing = isEditing,
                    onDelete = viewModel::deleteExperience,
                )
                ProfileListSection(
                    title = "Education",
                    items = state.educationList.map { it.id to "${it.degree} • ${it.institution}" },
                    isEditing = isEditing,
                    onDelete = viewModel::deleteEducation,
                )
                ProfileListSection(
                    title = "Languages",
                    items = state.languages.map { it.id to "${it.name}${it.proficiency?.let { p -> " ($p)" } ?: ""}" },
                    isEditing = isEditing,
                    onDelete = viewModel::deleteLanguage,
                )
                ProfileListSection(
                    title = "Certifications",
                    items = state.certifications.map { it.id to "${it.name} • ${it.issuingOrg}" },
                    isEditing = isEditing,
                    onDelete = viewModel::deleteCertification,
                )

                SaasPrimaryButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Log out", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddSkillDialog) {
        AddSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onAdd = {
                viewModel.addSkill(it)
                showAddSkillDialog = false
            },
        )
    }
}

@Composable
private fun ProfileHeader(profile: Profile?, completeness: Int) {
    SaasCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = AccentTeal, shape = CircleShape, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile?.fullName?.firstOrNull()?.uppercase() ?: "J",
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(profile?.fullName?.ifBlank { "Your profile" } ?: "Your profile", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
                Text(profile?.desiredRole ?: "Add a desired role", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { completeness / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = AccentTeal,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Text("$completeness% complete", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EditableProfileFields(
    fullName: String,
    onFullName: (String) -> Unit,
    phone: String,
    onPhone: (String) -> Unit,
    location: String,
    onLocation: (String) -> Unit,
    linkedinUrl: String,
    onLinkedIn: (String) -> Unit,
    desiredRole: String,
    onDesiredRole: (String) -> Unit,
    summary: String,
    onSummary: (String) -> Unit,
) {
    OutlinedTextField(fullName, onFullName, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(phone, onPhone, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(location, onLocation, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(linkedinUrl, onLinkedIn, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(desiredRole, onDesiredRole, label = { Text("Desired role") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(summary, onSummary, label = { Text("Professional summary") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
}

@Composable
private fun ProfileField(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(Modifier.padding(vertical = 5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProfileListSection(
    title: String,
    items: List<Pair<String, String>>,
    isEditing: Boolean,
    onDelete: (String) -> Unit,
) {
    SaasCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text("No ${title.lowercase()} added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { (id, label) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (isEditing) {
                        IconButton(onClick = { onDelete(id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSkillDialog(onDismiss: () -> Unit, onAdd: (Skill) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var proficiency by rememberSaveable { mutableStateOf("") }
    var years by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add skill") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Skill name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(proficiency, { proficiency = it }, label = { Text("Proficiency") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(years, { years = it }, label = { Text("Years") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(Skill(name = name, category = category.ifBlank { null }, proficiency = proficiency.ifBlank { null }, yearsExperience = years.toIntOrNull()))
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun skillLabel(skill: Skill): String = buildString {
    append(skill.name)
    skill.proficiency?.let { append(" ($it)") }
    skill.yearsExperience?.let { append(" ${it}y") }
}
