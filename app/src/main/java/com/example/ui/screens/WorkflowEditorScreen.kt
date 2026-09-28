package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExecutionStatus
import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode
import com.example.ui.components.AddNodeDialog
import com.example.ui.components.ArchitectureGuideDialog
import com.example.ui.components.CanvasGridAndWires
import com.example.ui.components.ExecutionHistorySheet
import com.example.ui.components.NodeCard
import com.example.ui.components.NodeConfigDialog
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowSecondary
import com.example.ui.viewmodel.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowEditorScreen(
    workflow: Workflow,
    viewModel: WorkflowViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Hardware/system back handler
    BackHandler {
        onNavigateBack()
    }

    val currentWorkflow by viewModel.currentWorkflow.collectAsStateWithLifecycle()
    val activeWorkflow = currentWorkflow ?: workflow

    val panOffset by viewModel.panOffset.collectAsStateWithLifecycle()
    val zoomScale by viewModel.zoomScale.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val connectingFrom by viewModel.connectingFrom.collectAsStateWithLifecycle()

    var selectedNodeToConfig by remember { mutableStateOf<WorkflowNode?>(null) }
    var selectedEdgeToDelete by remember { mutableStateOf<WorkflowEdge?>(null) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeWorkflow.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${activeWorkflow.nodes.size} nodes • ${activeWorkflow.edges.size} connections",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_to_list")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Guide Dialog Button
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.testTag("btn_editor_architecture_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Architecture Guide",
                            tint = FlowSecondary
                        )
                    }

                    // Execution Logs Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("btn_open_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Execution Logs",
                            tint = Color.White
                        )
                    }

                    // Execute Workflow Button
                    val isRunning = executionState.overallStatus == ExecutionStatus.RUNNING
                    Button(
                        onClick = {
                            viewModel.executeCurrentWorkflow()
                        },
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_run_workflow")
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Running...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNodeDialog = true },
                containerColor = FlowSecondary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_node")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Node")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Canvas View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.updatePanAndZoom(pan, zoom)
                        }
                    }
            ) {
                // Background grid and wires
                CanvasGridAndWires(
                    nodes = activeWorkflow.nodes,
                    edges = activeWorkflow.edges,
                    panOffset = panOffset,
                    zoomScale = zoomScale,
                    activeNodeIds = executionState.activeNodeIds,
                    completedNodeIds = executionState.completedNodeIds,
                    onEdgeClicked = { edge ->
                        selectedEdgeToDelete = edge
                    }
                )

                // Overlaid interactive Node Cards
                activeWorkflow.nodes.forEach { node ->
                    val status = when {
                        executionState.activeNodeIds.contains(node.id) -> ExecutionStatus.RUNNING
                        executionState.completedNodeIds.contains(node.id) -> ExecutionStatus.SUCCESS
                        executionState.failedNodeIds.contains(node.id) -> ExecutionStatus.FAILED
                        else -> ExecutionStatus.IDLE
                    }

                    NodeCard(
                        node = node,
                        executionStatus = status,
                        panOffset = panOffset,
                        zoomScale = zoomScale,
                        isConnectingSource = connectingFrom?.first == node.id,
                        onNodeDrag = { dx, dy ->
                            viewModel.updateNodePosition(node.id, dx, dy)
                        },
                        onNodeClicked = {
                            selectedNodeToConfig = node
                        },
                        onConfigureClicked = {
                            selectedNodeToConfig = node
                        },
                        onDeleteClicked = {
                            viewModel.deleteNode(node.id)
                        },
                        onOutputPortClicked = { portName ->
                            viewModel.startConnecting(node.id, portName)
                        },
                        onInputPortClicked = { portName ->
                            if (connectingFrom != null) {
                                viewModel.completeConnecting(node.id, portName)
                            }
                        }
                    )
                }
            }

            // Connecting status helper banner
            AnimatedVisibility(
                visible = connectingFrom != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                val fromNode = activeWorkflow.nodes.find { it.id == connectingFrom?.first }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2638),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlowSecondary),
                    modifier = Modifier.clickable { viewModel.cancelConnecting() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FlowSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connecting from ${fromNode?.name ?: "Node"}: Tap target input port (left dot), or tap here to cancel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Floating Canvas Zoom & View Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingControlIcon(icon = Icons.Default.Add, contentDescription = "Zoom In", testTag = "btn_zoom_in") {
                    viewModel.zoomIn()
                }
                FloatingControlIcon(icon = Icons.Default.Remove, contentDescription = "Zoom Out", testTag = "btn_zoom_out") {
                    viewModel.zoomOut()
                }
                FloatingControlIcon(icon = Icons.Default.CenterFocusStrong, contentDescription = "Reset View", testTag = "btn_reset_view") {
                    viewModel.resetView()
                }
            }
        }
    }

    // Node Config Dialog
    selectedNodeToConfig?.let { node ->
        NodeConfigDialog(
            node = node,
            onDismiss = { selectedNodeToConfig = null },
            onSaveConfig = { updated ->
                viewModel.updateNodeConfig(updated)
                selectedNodeToConfig = null
            },
            onTestNode = { testNode ->
                viewModel.testSingleNode(testNode)
            }
        )
    }

    // Add Node Dialog
    if (showAddNodeDialog) {
        AddNodeDialog(
            onDismiss = { showAddNodeDialog = false },
            onNodeTypeSelected = { type ->
                viewModel.addNode(type)
                showAddNodeDialog = false
            }
        )
    }

    // Execution History Sheet
    if (showHistorySheet) {
        ExecutionHistorySheet(
            executionState = executionState,
            onDismiss = { showHistorySheet = false }
        )
    }

    // Edge Delete Confirmation Dialog
    selectedEdgeToDelete?.let { edge ->
        AlertDialog(
            onDismissRequest = { selectedEdgeToDelete = null },
            title = { Text("Disconnect Nodes?", color = Color.White) },
            text = { Text("Do you want to delete this connection wire?", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEdge(edge)
                        selectedEdgeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Connection")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEdgeToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131927)
        )
    }

    // Architecture Guide Dialog
    if (showGuideDialog) {
        ArchitectureGuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun FloatingControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF131927),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .size(38.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
