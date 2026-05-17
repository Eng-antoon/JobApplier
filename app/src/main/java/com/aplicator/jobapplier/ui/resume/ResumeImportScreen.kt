package com.aplicator.jobapplier.ui.resume

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aplicator.jobapplier.ui.components.PremiumButton
import com.aplicator.jobapplier.ui.components.PremiumLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeImportScreen(
    onBack: () -> Unit,
    viewModel: ResumeImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.selectFile(it, context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Resume") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(200)))
            },
            label = "resumeImportState",
        ) { currentState ->
            when (currentState) {
                is ResumeImportState.Idle -> IdleContent(
                    onPickFile = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            ),
                        )
                    },
                )
                is ResumeImportState.Extracting -> LoadingContent("Extracting text from file...")
                is ResumeImportState.Parsing -> LoadingContent("AI is analyzing your resume...")
                is ResumeImportState.Preview -> PreviewContent(
                    data = currentState.data,
                    onToggleSkill = viewModel::toggleSkill,
                    onToggleExperience = viewModel::toggleExperience,
                    onToggleEducation = viewModel::toggleEducation,
                    onToggleCertification = viewModel::toggleCertification,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onTogglePersonalInfo = viewModel::togglePersonalInfo,
                    onConfirm = viewModel::confirmImport,
                )
                is ResumeImportState.Importing -> LoadingContent("Importing to your profile...")
                is ResumeImportState.Done -> DoneContent(onBack = onBack)
                is ResumeImportState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onPickFile: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Import Your Resume",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Upload a PDF or DOCX file and AI will extract your skills, experience, education, and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                PremiumButton(onClick = onPickFile) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose File")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Supported: PDF, DOCX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLoadingIndicator(message = message)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewContent(
    data: ResumePreviewData,
    onToggleSkill: (Int) -> Unit,
    onToggleExperience: (Int) -> Unit,
    onToggleEducation: (Int) -> Unit,
    onToggleCertification: (Int) -> Unit,
    onToggleLanguage: (Int) -> Unit,
    onTogglePersonalInfo: () -> Unit,
    onConfirm: () -> Unit,
) {
    val response = data.response

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Review Extracted Data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Uncheck items you don't want to import",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // Personal Info
        if (response.fullName != null || response.email != null || response.phone != null) {
            SectionCard(
                title = "Personal Info",
                checked = data.importPersonalInfo,
                onToggle = onTogglePersonalInfo,
            ) {
                response.fullName?.let { InfoRow("Name", it) }
                response.email?.let { InfoRow("Email", it) }
                response.phone?.let { InfoRow("Phone", it) }
                response.location?.let { InfoRow("Location", it) }
                response.linkedinUrl?.let { InfoRow("LinkedIn", it) }
                response.summary?.let { InfoRow("Summary", it) }
                response.desiredRole?.let { InfoRow("Desired Role", it) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Skills
        if (response.skills.isNotEmpty()) {
            Text(
                "Skills (${data.selectedSkills.count { it }}/${response.skills.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            response.skills.forEachIndexed { index, skill ->
                ItemRow(
                    checked = data.selectedSkills[index],
                    onToggle = { onToggleSkill(index) },
                    title = skill.name,
                    subtitle = listOfNotNull(
                        skill.category,
                        skill.proficiency,
                        skill.yearsExperience?.let { "${it}y" },
                    ).joinToString(" • "),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Experience
        if (response.experiences.isNotEmpty()) {
            Text(
                "Experience (${data.selectedExperiences.count { it }}/${response.experiences.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            response.experiences.forEachIndexed { index, exp ->
                ItemRow(
                    checked = data.selectedExperiences[index],
                    onToggle = { onToggleExperience(index) },
                    title = "${exp.title} at ${exp.company}",
                    subtitle = listOfNotNull(
                        exp.startDate,
                        exp.endDate ?: if (exp.isCurrent) "Present" else null,
                    ).joinToString(" - "),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Education
        if (response.education.isNotEmpty()) {
            Text(
                "Education (${data.selectedEducation.count { it }}/${response.education.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            response.education.forEachIndexed { index, edu ->
                ItemRow(
                    checked = data.selectedEducation[index],
                    onToggle = { onToggleEducation(index) },
                    title = "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""}",
                    subtitle = edu.institution,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Certifications
        if (response.certifications.isNotEmpty()) {
            Text(
                "Certifications (${data.selectedCertifications.count { it }}/${response.certifications.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            response.certifications.forEachIndexed { index, cert ->
                ItemRow(
                    checked = data.selectedCertifications[index],
                    onToggle = { onToggleCertification(index) },
                    title = cert.name,
                    subtitle = cert.issuingOrg ?: "",
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Languages
        if (response.languages.isNotEmpty()) {
            Text(
                "Languages (${data.selectedLanguages.count { it }}/${response.languages.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            response.languages.forEachIndexed { index, lang ->
                ItemRow(
                    checked = data.selectedLanguages[index],
                    onToggle = { onToggleLanguage(index) },
                    title = lang.name,
                    subtitle = lang.proficiency ?: "",
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))

        PremiumButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = data.selectedCount > 0,
        ) {
            Text("Import Selected (${data.selectedCount} items)")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (checked) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ItemRow(
    checked: Boolean,
    onToggle: () -> Unit,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DoneContent(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Import Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your profile has been updated with the imported data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PremiumButton(onClick = onBack) {
                Text("Back to Profile")
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}
