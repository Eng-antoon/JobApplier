package com.aplicator.jobapplier.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplicator.jobapplier.ui.components.generatedContentLabel
import com.aplicator.jobapplier.ui.snippets.SnippetItem

private sealed interface BubblePage {
    data class TopLevel(val page: BubblePanelPage) : BubblePage
    data class JobDetail(val job: BubbleJobItem) : BubblePage
}

@Composable
fun BubbleExpandedPanel(
    userName: String,
    snippets: List<SnippetItem>,
    recentJobs: List<BubbleJobItem>,
    generatedContent: Map<String, List<BubbleContentItem>>,
    onClose: () -> Unit,
    onCopy: (String, String) -> Unit,
    onOpenApp: () -> Unit,
    onAddJob: () -> Unit,
    onDismissBubble: () -> Unit,
    onRefresh: () -> Unit,
    generatingActionKey: String?,
    onGenerate: (BubbleJobItem, BubbleAiAction, String?) -> Unit,
) {
    var page by remember { mutableStateOf<BubblePage>(BubblePage.TopLevel(DefaultBubblePanelPage)) }
    val tabs = listOf(
        BubblePanelPage.SmartFill to "Smart Fill",
        BubblePanelPage.QuickCopy to "Quick Copy",
        BubblePanelPage.Actions to "Actions",
    )
    val firstName = userName.split(" ").firstOrNull() ?: userName
    val topLevelPage = (page as? BubblePage.TopLevel)?.page

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val detailPage = page as? BubblePage.JobDetail
                if (detailPage != null) {
                    IconButton(onClick = { page = BubblePage.TopLevel(BubblePanelPage.SmartFill) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF12324A), Color(0xFF0F766E), Color(0xFF06B6D4)),
                                    start = Offset.Zero,
                                    end = Offset(60f, 60f),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = firstName.firstOrNull()?.uppercase() ?: "J",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = detailPage?.job?.roleTitle ?: "Hi, $firstName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = detailPage?.job?.companyName ?: "Quick fill assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (topLevelPage != null) {
            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == topLevelPage }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEach { (tabPage, title) ->
                    Tab(
                        selected = topLevelPage == tabPage,
                        onClick = { page = BubblePage.TopLevel(tabPage) },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (topLevelPage == tabPage) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        }

        when (val currentPage = page) {
            is BubblePage.TopLevel -> when (currentPage.page) {
                BubblePanelPage.SmartFill -> SmartFillTab(
                    snippets = snippets,
                    recentJobs = recentJobs,
                    onCopy = onCopy,
                    onJobClick = { page = BubblePage.JobDetail(it) },
                )
                BubblePanelPage.QuickCopy -> QuickCopyTab(snippets = snippets, onCopy = onCopy)
                BubblePanelPage.Actions -> ActionsTab(
                    onOpenApp = onOpenApp,
                    onAddJob = onAddJob,
                    onDismissBubble = onDismissBubble,
                    onRefresh = onRefresh,
                )
            }
            is BubblePage.JobDetail -> BubbleJobDetailPage(
                job = currentPage.job,
                content = generatedContent[currentPage.job.jobId] ?: emptyList(),
                generatingActionKey = generatingActionKey,
                onCopy = onCopy,
                onGenerate = onGenerate,
            )
        }
    }
}

@Composable
private fun QuickCopyTab(
    snippets: List<SnippetItem>,
    onCopy: (String, String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val grouped = snippets
        .filter {
            searchQuery.isBlank() ||
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true)
        }
        .groupBy { it.category }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search snippets...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        if (snippets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No profile data yet. Add data in the app first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                val sortedCategories = grouped.keys.toList().sortedBy {
                    if (it == "personal") 0 else 1
                }

                sortedCategories.forEach { category ->
                    val items = grouped[category] ?: return@forEach

                    item(key = "header_$category") {
                        CollapsibleCategoryHeader(
                            category = category,
                            items = items,
                            onCopy = onCopy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleCategoryHeader(
    category: String,
    items: List<SnippetItem>,
    onCopy: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(category == "personal") }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { snippet ->
                    SnippetCardRow(snippet = snippet, onCopy = onCopy)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SnippetCardRow(
    snippet: SnippetItem,
    onCopy: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCopy(snippet.label, snippet.value) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snippet.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = snippet.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SmartFillTab(
    snippets: List<SnippetItem>,
    recentJobs: List<BubbleJobItem>,
    onCopy: (String, String) -> Unit,
    onJobClick: (BubbleJobItem) -> Unit,
) {
    val quickFields = snippets.filter {
        it.category == "personal" && it.label in listOf(
            "Full Name", "Email", "Phone", "LinkedIn", "Location",
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        // Quick Fields row
        if (quickFields.isNotEmpty()) {
            item(key = "quick_fields") {
                Text(
                    "Quick Fields",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickFields.forEach { field ->
                        FilledTonalButton(
                            onClick = { onCopy(field.label, field.value) },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(field.label, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Recent Jobs
        item(key = "jobs_header") {
            Text(
                "Recent Jobs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }

        if (recentJobs.isEmpty()) {
            item(key = "jobs_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.WorkOutline,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No jobs yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Add a job in the app to get AI-generated content",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        } else {
            items(recentJobs, key = { it.jobId }) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun JobCard(
    job: BubbleJobItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.roleTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = job.companyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            job.matchScore?.let { score ->
                val scoreColor = when {
                    score >= 70 -> Color(0xFF057642)
                    score >= 40 -> Color(0xFFB24020)
                    else -> Color(0xFFCC1016)
                }
                Text(
                    text = "${score}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BubbleJobDetailPage(
    job: BubbleJobItem,
    content: List<BubbleContentItem>,
    generatingActionKey: String?,
    onCopy: (String, String) -> Unit,
    onGenerate: (BubbleJobItem, BubbleAiAction, String?) -> Unit,
) {
    var customQuestion by remember(job.jobId) { mutableStateOf("") }
    val actions = defaultBubbleAiActions()
    val standardActions = actions.filterNot { it.contentType == "custom_question" }
    val customAction = actions.first { it.contentType == "custom_question" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "application_data_header") {
            SectionLabel("Application Data")
        }
        items(job.applicationCopyItems(), key = { it.label }) { item ->
            BubbleCopyRow(
                label = item.label,
                preview = item.value,
                onClick = { onCopy(item.label, item.value) },
            )
        }

        item(key = "generated_header") {
            SectionLabel("Crafted Content")
        }

        if (content.isEmpty()) {
            item(key = "generated_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Generate content for this job, then tap any row to copy it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(content, key = { "${it.contentType}:${it.createdAt}:${it.content.hashCode()}" }) { item ->
                val label = generatedContentLabel(item.contentType)
                BubbleCopyRow(
                    label = label,
                    preview = item.content,
                    onClick = { onCopy(label, item.content) },
                )
            }
        }

        item(key = "actions_header") {
            SectionLabel("Generate")
        }
        items(standardActions, key = { it.contentType }) { action ->
            GenerateActionButton(
                action = action,
                isLoading = generatingActionKey == bubbleActionKey(job.jobId, action.contentType),
                onClick = { onGenerate(job, action, action.question) },
            )
        }

        item(key = "custom_question") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    placeholder = { Text("Type a custom question...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                GenerateActionButton(
                    action = customAction,
                    isLoading = generatingActionKey == bubbleActionKey(job.jobId, customAction.contentType),
                    enabled = customQuestion.isNotBlank(),
                    onClick = { onGenerate(job, customAction, customQuestion.trim()) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
}

@Composable
private fun GenerateActionButton(
    action: BubbleAiAction,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(if (isLoading) "Generating..." else action.label, fontSize = 13.sp)
    }
}

@Composable
private fun BubbleCopyRow(
    label: String,
    preview: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ActionsTab(
    onOpenApp: () -> Unit,
    onAddJob: () -> Unit,
    onDismissBubble: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        ActionButton(
            label = "Open App",
            icon = { Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    containerColor = Color(0xFF12324A),
            contentColor = Color.White,
            onClick = onOpenApp,
        )

        ActionButton(
            label = "Add New Job",
            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onAddJob,
        )

        ActionButton(
            label = "Refresh Data",
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp)) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onRefresh,
        )

        Spacer(Modifier.weight(1f))

        ActionButton(
            label = "Dismiss Bubble",
            icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp)) },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onClick = onDismissBubble,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: @Composable () -> Unit,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides contentColor,
                ) {
                    icon()
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}
