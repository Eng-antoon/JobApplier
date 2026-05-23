package com.aplicator.jobapplier.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuotaExceededDialog(
    quotaInfo: QuotaExceededResponse,
    onRequestExtra: () -> Unit,
    onDismiss: () -> Unit,
    requestPending: Boolean = false,
) {
    val isResumeQuota = quotaInfo.quotaType == "resume"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
        },
        title = {
            Text(
                text = if (isResumeQuota) "Resume Parse Limit Reached" else "Weekly AI Limit Reached",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isResumeQuota) {
                    val progress = quotaInfo.used.toFloat() / quotaInfo.limit.coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${quotaInfo.used} / ${quotaInfo.limit} weekly AI actions used",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val weeklyExtras = quotaInfo.weeklyExtraRemaining ?: quotaInfo.extraRemaining
                    if (weeklyExtras > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$weeklyExtras approved extra weekly actions remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (quotaInfo.resetsAt != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val resetDate = try {
                            val instant = Instant.parse(quotaInfo.resetsAt)
                            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
                                .withZone(ZoneId.systemDefault())
                            formatter.format(instant)
                        } catch (_: Exception) {
                            "next Monday"
                        }
                        Text(
                            text = "Resets on $resetDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "You've used ${quotaInfo.used} / ${quotaInfo.limit} included resume parses.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Request 2 extra resume parses from the app owner to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val resumeExtras = quotaInfo.resumeExtraRemaining ?: quotaInfo.extraRemaining
                    if (resumeExtras > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$resumeExtras approved extra resume ${if (resumeExtras == 1) "parse" else "parses"} remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (requestPending) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isResumeQuota) {
                            "Your request for 2 resume parses is pending approval. After approval, close this and retry the import."
                        } else {
                            "Your request for 10 weekly AI actions is pending approval. After approval, close this and retry the action."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            if (quotaInfo.canRequestExtra && !requestPending) {
                Button(onClick = onRequestExtra) {
                    Text(quotaRequestActionLabel(quotaInfo.quotaType))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (quotaInfo.canRequestExtra && !requestPending) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        },
    )
}

@Composable
fun ResumeParseWarningDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp),
            )
        },
        title = {
            Text(
                text = "Last Free Resume Parse Used",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "You've used your last free resume parse. To parse another resume in the future, you'll need to request additional quota from the app owner.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("I Understand")
            }
        },
    )
}

@Composable
fun QuotaRequestSuccessDialog(
    quotaType: String = "weekly",
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Request Sent",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = quotaRequestSuccessMessage(quotaType),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
