package com.aplicator.jobapplier.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.service.BubbleOverlayService
import com.aplicator.jobapplier.ui.components.CompanyAvatar
import com.aplicator.jobapplier.ui.components.EmptyState
import com.aplicator.jobapplier.ui.components.MatchScoreRing
import com.aplicator.jobapplier.ui.components.MetricCard
import com.aplicator.jobapplier.ui.components.OverlayPermissionDialog
import com.aplicator.jobapplier.ui.components.SaasCard
import com.aplicator.jobapplier.ui.components.SaasPrimaryButton
import com.aplicator.jobapplier.ui.components.SaasScreenBackground
import com.aplicator.jobapplier.ui.components.SaasSearchField
import com.aplicator.jobapplier.ui.components.SelectableSaasChip
import com.aplicator.jobapplier.ui.components.ShimmerJobCard
import com.aplicator.jobapplier.ui.components.StatusPill
import com.aplicator.jobapplier.ui.components.buildDashboardMetrics
import com.aplicator.jobapplier.ui.components.filterJobs
import com.aplicator.jobapplier.ui.job.JobViewModel
import com.aplicator.jobapplier.ui.theme.AccentCyan
import com.aplicator.jobapplier.ui.theme.AccentTeal
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: JobViewModel,
    onAddJob: () -> Unit,
    onJobClick: (String) -> Unit,
) {
    val state by viewModel.jobListState.collectAsState()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("All") }

    if (showPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onGranted = { showPermissionDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Applications", fontWeight = FontWeight.Bold)
                        Text(
                            "Track, analyze, and apply faster",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                context.startForegroundService(Intent(context, BubbleOverlayService::class.java))
                            } else {
                                showPermissionDialog = true
                            }
                        },
                    ) {
                        Icon(Icons.Default.BubbleChart, contentDescription = "Launch Bubble")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddJob, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "New Application", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { padding ->
        SaasScreenBackground(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(4) { ShimmerJobCard() }
                }
                return@SaasScreenBackground
            }

            if (state.jobs.isEmpty()) {
                EmptyState(
                    title = "No applications yet",
                    message = "Add your first job description and JobApplier will score the fit and draft tailored content.",
                    icon = Icons.Default.WorkOutline,
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        SaasPrimaryButton(onClick = onAddJob) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Add application")
                        }
                    },
                )
                return@SaasScreenBackground
            }

            val metrics = buildDashboardMetrics(state.jobs)
            val filtered = filterJobs(state.jobs, searchQuery).filter {
                statusFilter == "All" || it.status.equals(statusFilter, ignoreCase = true)
            }
            val statuses = listOf("All") + state.jobs.map { it.status }.distinct()

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    SaasCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            "Your job search command center",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Prioritize high-match roles and keep copy-ready application material close.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            label = "Applications",
                            value = metrics.totalApplications.toString(),
                            icon = Icons.Default.WorkOutline,
                            accent = AccentTeal,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Avg match",
                            value = "${metrics.averageMatch}%",
                            icon = Icons.Default.Speed,
                            accent = AccentCyan,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SaasSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search company, role, or status",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        statuses.forEach { status ->
                            SelectableSaasChip(
                                label = if (status == "All") "All" else status.replaceFirstChar { it.uppercase() },
                                selected = statusFilter == status,
                                onClick = { statusFilter = status },
                            )
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No matching applications",
                            message = "Adjust your search or status filter.",
                            icon = Icons.Default.Search,
                        )
                    }
                } else {
                    itemsIndexed(filtered, key = { _, job -> job.id }) { index, job ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(job.id) {
                            delay(index * 45L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 5 },
                        ) {
                            JobCard(job = job, onClick = { onJobClick(job.id) })
                        }
                    }
                }
                item { Spacer(Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun JobCard(job: JobDescription, onClick: () -> Unit) {
    SaasCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanyAvatar(job.companyName)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    job.roleTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    job.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                StatusPill(job.status)
            }
            job.matchScore?.let { score ->
                MatchScoreRing(score)
            } ?: Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
