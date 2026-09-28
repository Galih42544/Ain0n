package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExecutionStatus
import com.example.data.model.NodeType
import com.example.data.model.WorkflowNode
import com.example.ui.theme.FlowPortFalse
import com.example.ui.theme.FlowPortInput
import com.example.ui.theme.FlowPortOutput
import com.example.ui.theme.FlowPortTrue
import com.example.ui.theme.FlowSecondary
import kotlin.math.roundToInt

@Composable
fun NodeCard(
    node: WorkflowNode,
    executionStatus: ExecutionStatus,
    panOffset: Offset,
    zoomScale: Float,
    isConnectingSource: Boolean,
    onNodeDrag: (dx: Float, dy: Float) -> Unit,
    onNodeClicked: () -> Unit,
    onConfigureClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onOutputPortClicked: (portName: String) -> Unit,
    onInputPortClicked: (portName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenX = (node.positionX * zoomScale + panOffset.x).roundToInt()
    val screenY = (node.positionY * zoomScale + panOffset.y).roundToInt()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val borderColor by animateColorAsState(
        targetValue = when (executionStatus) {
            ExecutionStatus.RUNNING -> FlowSecondary.copy(alpha = pulseAlpha)
            ExecutionStatus.SUCCESS -> Color(0xFF10B981)
            ExecutionStatus.FAILED -> Color(0xFFEF4444)
            else -> if (isConnectingSource) FlowSecondary else Color(0xFF334155)
        },
        label = "border_color"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(screenX, screenY) }
            .width((250 * zoomScale).dp.coerceAtLeast(180.dp))
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onNodeDrag(dragAmount.x / zoomScale, dragAmount.y / zoomScale)
                }
            }
            .testTag("node_${node.id}")
    ) {
        // Main Node Body Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (executionStatus == ExecutionStatus.RUNNING) 2.5.dp else 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onNodeClicked() },
            color = Color(0xFF131927),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(node.type.category.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (node.type) {
                                    NodeType.TRIGGER_MANUAL, NodeType.TRIGGER_WEBHOOK -> Icons.Default.PlayArrow
                                    NodeType.AI_ROUTER, NodeType.IF_CONDITION -> Icons.Default.Settings
                                    else -> Icons.Default.SmartToy
                                },
                                contentDescription = null,
                                tint = node.type.category.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = node.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.type.category.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = node.type.category.color
                            )
                        }
                    }

                    // Status Indicator
                    when (executionStatus) {
                        ExecutionStatus.RUNNING -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Running",
                                tint = FlowSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        ExecutionStatus.SUCCESS -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        ExecutionStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val previewText = when (node.type) {
                        NodeType.TRIGGER_MANUAL, NodeType.TRIGGER_WEBHOOK -> {
                            "Payload: ${node.config.initialPayload.take(30)}..."
                        }
                        NodeType.AI_AGENT, NodeType.AI_RESEARCHER, NodeType.AI_EVALUATOR -> {
                            "${node.config.modelName} (temp: ${node.config.temperature})"
                        }
                        NodeType.IF_CONDITION -> {
                            "if ${node.config.conditionField} == '${node.config.conditionValue}'"
                        }
                        NodeType.TRANSFORMER_JSON -> {
                            "extract '${node.config.jsonExtractPath}'"
                        }
                        NodeType.ACTION_HTTP -> {
                            "${node.config.httpMethod} ${node.config.httpUrl.take(20)}..."
                        }
                        else -> node.type.description
                    }

                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Card Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onConfigureClicked,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("node_config_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Node",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClicked,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("node_delete_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Node",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Input Port Handle (Left edge)
        if (node.type.inputPorts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-8).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.5.dp, FlowPortInput, CircleShape)
                    .clickable { onInputPortClicked("input") }
                    .testTag("input_port_${node.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FlowPortInput)
                )
            }
        }

        // Output Port Handle(s) (Right edge)
        if (node.type.outputPorts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                node.type.outputPorts.forEach { portName ->
                    val portColor = when (portName) {
                        "true" -> FlowPortTrue
                        "false" -> FlowPortFalse
                        else -> FlowPortOutput
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, portColor, CircleShape)
                            .clickable { onOutputPortClicked(portName) }
                            .testTag("output_port_${node.id}_$portName"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(portColor)
                        )
                    }
                }
            }
        }
    }
}
