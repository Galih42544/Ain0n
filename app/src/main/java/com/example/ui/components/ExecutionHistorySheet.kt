package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExecutionStatus
import com.example.data.model.NodeStepLog
import com.example.data.model.WorkflowExecutionState
import com.example.ui.theme.FlowPortInput
import com.example.ui.theme.FlowPortOutput
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionHistorySheet(
    executionState: WorkflowExecutionState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131927),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Workflow Execution Logs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (executionState.overallStatus) {
                                        ExecutionStatus.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        ExecutionStatus.FAILED -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        ExecutionStatus.RUNNING -> FlowSecondary.copy(alpha = 0.2f)
                                        else -> Color(0xFF334155)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = executionState.overallStatus.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (executionState.overallStatus) {
                                    ExecutionStatus.SUCCESS -> Color(0xFF10B981)
                                    ExecutionStatus.FAILED -> Color(0xFFEF4444)
                                    ExecutionStatus.RUNNING -> FlowSecondary
                                    else -> Color.White
                                }
                            )
                        }

                        if (executionState.totalDurationMs > 0) {
                            Text(
                                text = "${executionState.totalDurationMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_execution_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            if (executionState.summaryError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = executionState.summaryError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Logs List
            val logsList = executionState.stepLogs.values.toList().sortedBy { it.startTime }

            if (logsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No step logs recorded yet. Tap 'Execute Workflow' to run.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logsList) { log ->
                        val isExpanded = expandedMap[log.nodeId] ?: false

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedMap[log.nodeId] = !isExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when (log.status) {
                                                ExecutionStatus.SUCCESS -> Icons.Default.CheckCircle
                                                ExecutionStatus.FAILED -> Icons.Default.Error
                                                ExecutionStatus.RUNNING -> Icons.Default.Refresh
                                                else -> Icons.Default.Schedule
                                            },
                                            contentDescription = null,
                                            tint = when (log.status) {
                                                ExecutionStatus.SUCCESS -> Color(0xFF10B981)
                                                ExecutionStatus.FAILED -> Color(0xFFEF4444)
                                                ExecutionStatus.RUNNING -> FlowSecondary
                                                else -> Color(0xFF94A3B8)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = log.nodeName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${log.nodeType.title} • ${log.durationMs}ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Input Section
                                    Text("INPUT DATA", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = FlowPortInput)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF090D16))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = log.inputData,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Output Section
                                    Text("OUTPUT DATA", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = FlowPortOutput)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF090D16))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = log.outputData.ifBlank { "(No output recorded)" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = Color(0xFFA5F3FC)
                                        )
                                    }

                                    if (log.errorMessage != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("ERROR", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                                        Text(
                                            text = log.errorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
