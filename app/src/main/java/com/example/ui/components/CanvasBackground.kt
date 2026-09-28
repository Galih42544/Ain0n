package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode
import com.example.ui.theme.FlowCanvasDot
import com.example.ui.theme.FlowPortFalse
import com.example.ui.theme.FlowPortInput
import com.example.ui.theme.FlowPortOutput
import com.example.ui.theme.FlowPortTrue
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowSecondary
import kotlin.math.abs

@Composable
fun CanvasGridAndWires(
    nodes: List<WorkflowNode>,
    edges: List<WorkflowEdge>,
    panOffset: Offset,
    zoomScale: Float,
    activeNodeIds: Set<String>,
    completedNodeIds: Set<String>,
    onEdgeClicked: (WorkflowEdge) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wire_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val nodeMap = nodes.associateBy { it.id }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(edges, panOffset, zoomScale) {
                detectTapGestures { tapOffset ->
                    // Check if tap hit near any edge curve
                    val worldTap = (tapOffset - panOffset) / zoomScale
                    val clickedEdge = edges.find { edge ->
                        val fromNode = nodeMap[edge.fromNodeId] ?: return@find false
                        val toNode = nodeMap[edge.toNodeId] ?: return@find false

                        val startX = fromNode.positionX + 240f
                        val startY = fromNode.positionY + 70f
                        val endX = toNode.positionX
                        val endY = toNode.positionY + 70f

                        val midX = (startX + endX) / 2f
                        val midY = (startY + endY) / 2f

                        val dist = (worldTap - Offset(midX, midY)).getDistance()
                        dist < 32f
                    }
                    if (clickedEdge != null) {
                        onEdgeClicked(clickedEdge)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw dot matrix background grid
        val gridSize = 28f * zoomScale
        val startX = (panOffset.x % gridSize + gridSize) % gridSize
        val startY = (panOffset.y % gridSize + gridSize) % gridSize

        var curX = startX
        while (curX < width) {
            var curY = startY
            while (curY < height) {
                drawCircle(
                    color = FlowCanvasDot,
                    radius = 1.6f * zoomScale.coerceAtLeast(0.6f),
                    center = Offset(curX, curY)
                )
                curY += gridSize
            }
            curX += gridSize
        }

        // 2. Draw connections (Bezier Curves)
        for (edge in edges) {
            val fromNode = nodeMap[edge.fromNodeId] ?: continue
            val toNode = nodeMap[edge.toNodeId] ?: continue

            // Compute port anchor positions in world coordinates
            val startWorldX = fromNode.positionX + 250f
            val startWorldY = fromNode.positionY + 68f

            val endWorldX = toNode.positionX
            val endWorldY = toNode.positionY + 68f

            // Transform to screen coordinates
            val p1 = Offset(
                x = startWorldX * zoomScale + panOffset.x,
                y = startWorldY * zoomScale + panOffset.y
            )
            val p2 = Offset(
                x = endWorldX * zoomScale + panOffset.x,
                y = endWorldY * zoomScale + panOffset.y
            )

            val deltaX = abs(p2.x - p1.x).coerceAtLeast(60f)
            val control1 = Offset(p1.x + deltaX * 0.5f, p1.y)
            val control2 = Offset(p2.x - deltaX * 0.5f, p2.y)

            val path = Path().apply {
                moveTo(p1.x, p1.y)
                cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
            }

            val isWireActive = activeNodeIds.contains(fromNode.id) || activeNodeIds.contains(toNode.id)
            val isWireCompleted = completedNodeIds.contains(fromNode.id) && completedNodeIds.contains(toNode.id)

            val baseColor = when (edge.fromPort) {
                "true" -> FlowPortTrue
                "false" -> FlowPortFalse
                else -> if (isWireCompleted) FlowSecondary else FlowPrimary.copy(alpha = 0.6f)
            }

            // Glow underlay if active
            if (isWireActive) {
                drawPath(
                    path = path,
                    color = FlowSecondary.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 8f * zoomScale,
                        cap = StrokeCap.Round
                    )
                )
            }

            // Main wire stroke
            val strokeEffect = if (isWireActive) {
                PathEffect.dashPathEffect(floatArrayOf(15f, 15f), phase)
            } else {
                null
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = if (isWireActive) listOf(FlowSecondary, FlowPrimary) else listOf(baseColor, baseColor.copy(alpha = 0.8f)),
                    startX = p1.x,
                    endX = p2.x
                ),
                style = Stroke(
                    width = (if (isWireActive) 3.5f else 2.5f) * zoomScale.coerceIn(0.7f, 1.5f),
                    cap = StrokeCap.Round,
                    pathEffect = strokeEffect
                )
            )

            // Draw center interactive badge for connection
            val midScreen = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            drawCircle(
                color = Color(0xFF1E2638),
                radius = 6f * zoomScale,
                center = midScreen
            )
            drawCircle(
                color = baseColor,
                radius = 3.5f * zoomScale,
                center = midScreen
            )
        }
    }
}
