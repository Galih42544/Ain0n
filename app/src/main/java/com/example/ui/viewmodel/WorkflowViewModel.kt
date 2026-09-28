package com.example.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WorkflowRepository
import com.example.data.db.ExecutionHistoryEntity
import com.example.data.model.ExecutionStatus
import com.example.data.model.NodeConfig
import com.example.data.model.NodeType
import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowExecutionState
import com.example.data.model.WorkflowNode
import com.example.engine.WorkflowExecutionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class WorkflowViewModel(
    private val repository: WorkflowRepository
) : ViewModel() {

    private val engine = WorkflowExecutionEngine()

    val workflows: StateFlow<List<Workflow>> = repository.allWorkflows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val executionHistory: StateFlow<List<ExecutionHistoryEntity>> = repository.recentExecutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWorkflow = MutableStateFlow<Workflow?>(null)
    val currentWorkflow: StateFlow<Workflow?> = _currentWorkflow.asStateFlow()

    val executionState: StateFlow<WorkflowExecutionState> = engine.executionState

    // Canvas transformation state
    private val _panOffset = MutableStateFlow(Offset(100f, 150f))
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    // Port linking state
    private val _connectingFrom = MutableStateFlow<Pair<String, String>?>(null) // (NodeId, PortName)
    val connectingFrom: StateFlow<Pair<String, String>?> = _connectingFrom.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedTemplatesIfEmpty()
        }
    }

    fun selectWorkflow(workflow: Workflow) {
        _currentWorkflow.value = workflow
        _panOffset.value = Offset(80f, 120f)
        _zoomScale.value = 1.0f
        _connectingFrom.value = null
    }

    fun selectWorkflowById(id: String) {
        viewModelScope.launch {
            val wf = repository.getWorkflowById(id)
            if (wf != null) {
                selectWorkflow(wf)
            }
        }
    }

    fun updatePanAndZoom(panChange: Offset, zoomChange: Float) {
        _panOffset.value += panChange
        _zoomScale.value = (_zoomScale.value * zoomChange).coerceIn(0.5f, 2.0f)
    }

    fun resetView() {
        _panOffset.value = Offset(80f, 120f)
        _zoomScale.value = 1.0f
    }

    fun zoomIn() {
        _zoomScale.value = (_zoomScale.value * 1.15f).coerceAtMost(2.0f)
    }

    fun zoomOut() {
        _zoomScale.value = (_zoomScale.value / 1.15f).coerceAtLeast(0.5f)
    }

    fun updateNodePosition(nodeId: String, dx: Float, dy: Float) {
        val current = _currentWorkflow.value ?: return
        val updatedNodes = current.nodes.map { node ->
            if (node.id == nodeId) {
                node.copy(
                    positionX = (node.positionX + dx).coerceAtLeast(20f),
                    positionY = (node.positionY + dy).coerceAtLeast(20f)
                )
            } else node
        }
        val updatedWorkflow = current.copy(nodes = updatedNodes, updatedAt = System.currentTimeMillis())
        _currentWorkflow.value = updatedWorkflow
        saveWorkflowDebounced(updatedWorkflow)
    }

    fun updateNodeConfig(updatedNode: WorkflowNode) {
        val current = _currentWorkflow.value ?: return
        val updatedNodes = current.nodes.map { if (it.id == updatedNode.id) updatedNode else it }
        val updatedWorkflow = current.copy(nodes = updatedNodes, updatedAt = System.currentTimeMillis())
        _currentWorkflow.value = updatedWorkflow
        viewModelScope.launch {
            repository.saveWorkflow(updatedWorkflow)
        }
    }

    fun addNode(type: NodeType) {
        val current = _currentWorkflow.value ?: return
        // Place new node slightly offset from current center
        val screenCenterX = (-_panOffset.value.x + 300f) / _zoomScale.value
        val screenCenterY = (-_panOffset.value.y + 350f) / _zoomScale.value

        val newNode = WorkflowNode(
            id = UUID.randomUUID().toString(),
            type = type,
            name = type.title,
            positionX = screenCenterX.coerceAtLeast(40f),
            positionY = screenCenterY.coerceAtLeast(40f),
            config = when (type) {
                NodeType.TRIGGER_MANUAL -> NodeConfig(initialPayload = "{\"input\": \"Hello World\"}")
                NodeType.AI_AGENT -> NodeConfig(promptTemplate = "Process this:\n{{\${'$'}json.output}}")
                else -> NodeConfig()
            }
        )
        val updatedNodes = current.nodes + newNode
        val updatedWorkflow = current.copy(nodes = updatedNodes, updatedAt = System.currentTimeMillis())
        _currentWorkflow.value = updatedWorkflow
        viewModelScope.launch {
            repository.saveWorkflow(updatedWorkflow)
        }
    }

    fun deleteNode(nodeId: String) {
        val current = _currentWorkflow.value ?: return
        val updatedNodes = current.nodes.filterNot { it.id == nodeId }
        val updatedEdges = current.edges.filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId }
        val updatedWorkflow = current.copy(
            nodes = updatedNodes,
            edges = updatedEdges,
            updatedAt = System.currentTimeMillis()
        )
        _currentWorkflow.value = updatedWorkflow
        viewModelScope.launch {
            repository.saveWorkflow(updatedWorkflow)
        }
    }

    fun startConnecting(nodeId: String, portName: String) {
        _connectingFrom.value = Pair(nodeId, portName)
    }

    fun completeConnecting(toNodeId: String, toPortName: String = "input") {
        val from = _connectingFrom.value ?: return
        val current = _currentWorkflow.value ?: return

        // Prevent self-connection
        if (from.first == toNodeId) {
            _connectingFrom.value = null
            return
        }

        // Prevent duplicate edge
        val exists = current.edges.any {
            it.fromNodeId == from.first && it.fromPort == from.second && it.toNodeId == toNodeId
        }

        if (!exists) {
            val newEdge = WorkflowEdge(
                id = UUID.randomUUID().toString(),
                fromNodeId = from.first,
                fromPort = from.second,
                toNodeId = toNodeId,
                toPort = toPortName
            )
            val updatedEdges = current.edges + newEdge
            val updatedWorkflow = current.copy(edges = updatedEdges, updatedAt = System.currentTimeMillis())
            _currentWorkflow.value = updatedWorkflow
            viewModelScope.launch {
                repository.saveWorkflow(updatedWorkflow)
            }
        }
        _connectingFrom.value = null
    }

    fun cancelConnecting() {
        _connectingFrom.value = null
    }

    fun deleteEdge(edge: WorkflowEdge) {
        val current = _currentWorkflow.value ?: return
        val updatedEdges = current.edges.filterNot { it.id == edge.id }
        val updatedWorkflow = current.copy(edges = updatedEdges, updatedAt = System.currentTimeMillis())
        _currentWorkflow.value = updatedWorkflow
        viewModelScope.launch {
            repository.saveWorkflow(updatedWorkflow)
        }
    }

    fun executeCurrentWorkflow() {
        val current = _currentWorkflow.value ?: return
        viewModelScope.launch {
            val result = engine.executeWorkflow(current)
            // Persist execution history
            val history = ExecutionHistoryEntity(
                id = result.executionId,
                workflowId = current.id,
                workflowName = current.name,
                status = result.overallStatus.name,
                totalDurationMs = result.totalDurationMs,
                startedAt = result.startedAt,
                completedAt = result.completedAt,
                stepLogsJson = result.stepLogs.values.joinToString { it.nodeName + ": " + it.status },
                summaryError = result.summaryError
            )
            repository.saveExecution(history)
        }
    }

    suspend fun testSingleNode(node: WorkflowNode): String {
        return try {
            val singleWorkflow = Workflow(
                id = "test-sandbox",
                name = "Sandbox",
                description = "Single node test",
                nodes = listOf(node),
                edges = emptyList()
            )
            val state = engine.executeWorkflow(singleWorkflow)
            val log = state.stepLogs[node.id]
            log?.outputData?.ifBlank { "Node executed successfully with no output payload." }
                ?: (log?.errorMessage ?: "Execution completed.")
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun createWorkflow(name: String, description: String) {
        val newWf = Workflow(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            nodes = listOf(
                WorkflowNode(
                    id = UUID.randomUUID().toString(),
                    type = NodeType.TRIGGER_MANUAL,
                    name = "Start Trigger",
                    positionX = 80f,
                    positionY = 180f,
                    config = NodeConfig(initialPayload = "{\"message\": \"Hello NodeFlow\"}")
                ),
                WorkflowNode(
                    id = UUID.randomUUID().toString(),
                    type = NodeType.AI_AGENT,
                    name = "AI Processor",
                    positionX = 420f,
                    positionY = 160f,
                    config = NodeConfig(
                        modelName = "gemini-3.5-flash",
                        systemInstruction = "You are a helpful assistant.",
                        promptTemplate = "Analyze: {{\${'$'}json.message}}"
                    )
                ),
                WorkflowNode(
                    id = UUID.randomUUID().toString(),
                    type = NodeType.ACTION_OUTPUT,
                    name = "Final Output",
                    positionX = 760f,
                    positionY = 180f
                )
            ),
            edges = emptyList(),
            isTemplate = false,
            category = "Custom"
        )
        viewModelScope.launch {
            repository.saveWorkflow(newWf)
            selectWorkflow(newWf)
        }
    }

    fun deleteWorkflow(workflowId: String) {
        viewModelScope.launch {
            repository.deleteWorkflow(workflowId)
            if (_currentWorkflow.value?.id == workflowId) {
                _currentWorkflow.value = null
            }
        }
    }

    private fun saveWorkflowDebounced(workflow: Workflow) {
        viewModelScope.launch {
            repository.saveWorkflow(workflow)
        }
    }
}

class WorkflowViewModelFactory(
    private val repository: WorkflowRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkflowViewModel(repository) as T
    }
}
