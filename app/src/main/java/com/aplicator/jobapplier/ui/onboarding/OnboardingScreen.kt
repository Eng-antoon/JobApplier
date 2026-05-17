package com.aplicator.jobapplier.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import com.aplicator.jobapplier.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch

private const val TOTAL_STEPS = 4

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: ProfileViewModel,
    onComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_STEPS })
    val scope = rememberCoroutineScope()

    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var linkedinUrl by rememberSaveable { mutableStateOf("") }
    var desiredRole by rememberSaveable { mutableStateOf("") }

    var skillName by rememberSaveable { mutableStateOf("") }
    var skillCategory by rememberSaveable { mutableStateOf("") }
    var skillProficiency by rememberSaveable { mutableStateOf("") }

    var company by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }

    var summary by rememberSaveable { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.profile) {
        val profile = uiState.profile ?: return@LaunchedEffect
        if (fullName.isEmpty()) fullName = profile.fullName
        if (phone.isEmpty()) phone = profile.phone ?: ""
        if (location.isEmpty()) location = profile.location ?: ""
        if (linkedinUrl.isEmpty()) linkedinUrl = profile.linkedinUrl ?: ""
        if (desiredRole.isEmpty()) desiredRole = profile.desiredRole ?: ""
        if (summary.isEmpty()) summary = profile.summary ?: ""
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / TOTAL_STEPS },
                modifier = Modifier.fillMaxWidth(),
            )

            // Page indicators (dots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(TOTAL_STEPS) { index ->
                    val isActive = index == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dotWidth",
                    )
                    val color by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(300),
                        label = "dotColor",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (page) {
                        0 -> {
                            Text("Welcome! Let's set up your profile", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Tell us about yourself", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (City, Country)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = linkedinUrl, onValueChange = { linkedinUrl = it }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = desiredRole, onValueChange = { desiredRole = it }, label = { Text("Desired Role") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        1 -> {
                            Text("Add Your Skills", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Add skills one at a time. You can always add more later.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = skillName, onValueChange = { skillName = it }, label = { Text("Skill Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = skillCategory, onValueChange = { skillCategory = it }, label = { Text("Category (programming, tool, soft_skill)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = skillProficiency, onValueChange = { skillProficiency = it }, label = { Text("Proficiency (beginner/intermediate/advanced/expert)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    if (skillName.isNotBlank()) {
                                        viewModel.addSkill(Skill(name = skillName, category = skillCategory.ifBlank { null }, proficiency = skillProficiency.ifBlank { null }))
                                        skillName = ""
                                        skillCategory = ""
                                        skillProficiency = ""
                                    }
                                },
                                enabled = skillName.isNotBlank(),
                            ) {
                                Text("Add Skill")
                            }
                            if (uiState.skills.isNotEmpty()) {
                                Spacer(Modifier.height(20.dp))
                                Text("Your Skills (${uiState.skills.size})", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    uiState.skills.forEach { skill ->
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(skill.name) },
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text("Work Experience", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Add your most recent position. You can add more later.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            if (uiState.experiences.isNotEmpty()) {
                                Spacer(Modifier.height(20.dp))
                                Text("Your Experience (${uiState.experiences.size})", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                uiState.experiences.forEach { exp ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(exp.title, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                            Text("${exp.company} • ${exp.startDate ?: ""} - ${exp.endDate ?: "Present"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            Text("Professional Summary", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Write a brief summary about yourself. This helps AI generate better content for you.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Professional Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10)
                        }
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Text("Back")
                    }
                } else {
                    TextButton(onClick = {
                        viewModel.markOnboarded()
                        onComplete()
                    }) {
                        Text("Skip")
                    }
                }

                if (pagerState.currentPage < TOTAL_STEPS - 1) {
                    Button(
                        onClick = {
                            if (pagerState.currentPage == 0 && fullName.isNotBlank()) {
                                viewModel.updateProfile(
                                    Profile(
                                        id = "",
                                        fullName = fullName,
                                        phone = phone.ifBlank { null },
                                        location = location.ifBlank { null },
                                        linkedinUrl = linkedinUrl.ifBlank { null },
                                        desiredRole = desiredRole.ifBlank { null },
                                    )
                                )
                            }
                            if (pagerState.currentPage == 2 && company.isNotBlank() && jobTitle.isNotBlank() && startDate.isNotBlank()) {
                                viewModel.addExperience(WorkExperience(company = company, title = jobTitle, startDate = startDate, isCurrent = true))
                            }
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        enabled = pagerState.currentPage != 0 || fullName.isNotBlank(),
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = {
                        if (summary.isNotBlank()) {
                            viewModel.updateProfile(
                                Profile(
                                    id = "",
                                    fullName = fullName,
                                    phone = phone.ifBlank { null },
                                    location = location.ifBlank { null },
                                    linkedinUrl = linkedinUrl.ifBlank { null },
                                    desiredRole = desiredRole.ifBlank { null },
                                    summary = summary.ifBlank { null },
                                )
                            )
                        }
                        viewModel.markOnboarded()
                        onComplete()
                    }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}
