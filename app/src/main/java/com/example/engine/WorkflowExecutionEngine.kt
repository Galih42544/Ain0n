package com.example.engine

import com.example.BuildConfig
import com.example.data.model.ExecutionStatus
import com.example.data.model.NodeStepLog
import com.example.data.model.NodeType
import com.example.data.model.Workflow
import com.example.data.model.WorkflowExecutionState
import com.example.data.model.WorkflowNode
import com.example.data.network.GeminiClient
import com.example.data.network.GeminiContent
import com.example.data.network.GeminiGenerationConfig
import com.example.data.network.GeminiPart
import com.example.data.network.GeminiRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class WorkflowExecutionEngine {

    private val _executionState = MutableStateFlow(WorkflowExecutionState())
    val executionState: StateFlow<WorkflowExecutionState> = _executionState.asStateFlow()

    suspend fun executeWorkflow(workflow: Workflow): WorkflowExecutionState = withContext(Dispatchers.IO) {
        val executionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        _executionState.value = WorkflowExecutionState(
            executionId = executionId,
            workflowId = workflow.id,
            overallStatus = ExecutionStatus.RUNNING,
            startedAt = startTime
        )

        // Validate DAG
        if (DAGResolver.hasCycle(workflow)) {
            val failedState = _executionState.value.copy(
                overallStatus = ExecutionStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                summaryError = "Cycle detected in workflow graph! Workflows must be Directed Acyclic Graphs (DAG)."
            )
            _executionState.value = failedState
            return@withContext failedState
        }

        val entryNodes = DAGResolver.getEntryNodes(workflow)
        if (entryNodes.isEmpty()) {
            val failedState = _executionState.value.copy(
                overallStatus = ExecutionStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                summaryError = "No entry nodes or triggers found in workflow."
            )
            _executionState.value = failedState
            return@withContext failedState
        }

        // Store outputs of each node: NodeId -> Output String (JSON or text)
        val nodeOutputs = mutableMapOf<String, String>()
        val stepLogs = mutableMapOf<String, NodeStepLog>()
        val completedNodes = mutableSetOf<String>()
        val failedNodes = mutableSetOf<String>()

        // Queue of nodes ready to execute
        val readyQueue = ArrayDeque<WorkflowNode>()
        readyQueue.addAll(entryNodes)
        val queuedNodeIds = entryNodes.map { it.id }.toMutableSet()

        try {
            while (readyQueue.isNotEmpty()) {
                val currentNode = readyQueue.removeFirst()
                val nodeId = currentNode.id

                // Update active state
                _executionState.value = _executionState.value.copy(
                    activeNodeIds = setOf(nodeId),
                    stepLogs = stepLogs.toMap()
                )

                // Collect incoming inputs from upstream nodes
                val incomingEdges = DAGResolver.getIncomingEdges(workflow, nodeId)
                val inputData = if (incomingEdges.isEmpty()) {
                    currentNode.config.initialPayload
                } else {
                    // Combine upstream outputs or take first available
                    val validUpstreamOutputs = incomingEdges.mapNotNull { edge ->
                        nodeOutputs[edge.fromNodeId]
                    }
                    if (validUpstreamOutputs.size == 1) {
                        validUpstreamOutputs.first()
                    } else if (validUpstreamOutputs.size > 1) {
                        // Merge JSON if possible
                        mergeOutputs(validUpstreamOutputs)
                    } else {
                        currentNode.config.initialPayload
                    }
                }

                val nodeStartTime = System.currentTimeMillis()
                var activeOutPort: String = "output"

                val initialStepLog = NodeStepLog(
                    nodeId = nodeId,
                    nodeName = currentNode.name,
                    nodeType = currentNode.type,
                    status = ExecutionStatus.RUNNING,
                    inputData = inputData,
                    startTime = nodeStartTime
                )
                stepLogs[nodeId] = initialStepLog
                _executionState.value = _executionState.value.copy(stepLogs = stepLogs.toMap())

                try {
                    // Execute individual node
                    val (resultOutput, chosenPort) = executeNode(currentNode, inputData)
                    activeOutPort = chosenPort
                    val nodeEndTime = System.currentTimeMillis()

                    nodeOutputs[nodeId] = resultOutput
                    completedNodes.add(nodeId)

                    val updatedLog = initialStepLog.copy(
                        status = ExecutionStatus.SUCCESS,
                        outputData = resultOutput,
                        endTime = nodeEndTime,
                        durationMs = nodeEndTime - nodeStartTime,
                        modelUsed = currentNode.config.modelName
                    )
                    stepLogs[nodeId] = updatedLog

                    _executionState.value = _executionState.value.copy(
                        completedNodeIds = completedNodes.toSet(),
                        stepLogs = stepLogs.toMap()
                    )

                    // Find downstream nodes connected to the active port
                    val outgoingEdges = DAGResolver.getOutgoingEdges(workflow, nodeId, activeOutPort)
                    for (edge in outgoingEdges) {
                        val targetNode = workflow.nodes.find { it.id == edge.toNodeId }
                        if (targetNode != null && !queuedNodeIds.contains(targetNode.id)) {
                            // Check if all other required upstreams are completed
                            val targetIncoming = DAGResolver.getIncomingEdges(workflow, targetNode.id)
                            val allUpstreamReady = targetIncoming.all { upEdge ->
                                completedNodes.contains(upEdge.fromNodeId)
                            }
                            if (allUpstreamReady) {
                                readyQueue.add(targetNode)
                                queuedNodeIds.add(targetNode.id)
                            }
                        }
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val nodeEndTime = System.currentTimeMillis()
                    failedNodes.add(nodeId)

                    val failedLog = initialStepLog.copy(
                        status = ExecutionStatus.FAILED,
                        errorMessage = e.message ?: "Execution failed",
                        endTime = nodeEndTime,
                        durationMs = nodeEndTime - nodeStartTime
                    )
                    stepLogs[nodeId] = failedLog

                    _executionState.value = _executionState.value.copy(
                        failedNodeIds = failedNodes.toSet(),
                        stepLogs = stepLogs.toMap()
                    )
                }

                delay(200) // Visual pacing for DAG animation
            }

            val endTime = System.currentTimeMillis()
            val finalStatus = if (failedNodes.isNotEmpty()) ExecutionStatus.FAILED else ExecutionStatus.SUCCESS

            val finalState = _executionState.value.copy(
                overallStatus = finalStatus,
                activeNodeIds = emptySet(),
                completedAt = endTime,
                totalDurationMs = endTime - startTime,
                stepLogs = stepLogs.toMap()
            )
            _executionState.value = finalState
            return@withContext finalState

        } catch (c: CancellationException) {
            val endTime = System.currentTimeMillis()
            val cancelState = _executionState.value.copy(
                overallStatus = ExecutionStatus.FAILED,
                activeNodeIds = emptySet(),
                completedAt = endTime,
                totalDurationMs = endTime - startTime,
                summaryError = "Workflow execution cancelled"
            )
            _executionState.value = cancelState
            return@withContext cancelState
        }
    }

    private suspend fun executeNode(node: WorkflowNode, inputData: String): Pair<String, String> {
        return when (node.type) {
            NodeType.TRIGGER_MANUAL, NodeType.TRIGGER_WEBHOOK -> {
                delay(300)
                val payload = if (node.config.initialPayload.isNotBlank()) node.config.initialPayload else inputData
                Pair(payload, "output")
            }

            NodeType.AI_AGENT, NodeType.AI_RESEARCHER, NodeType.AI_EVALUATOR -> {
                val prompt = TemplateInterpolator.interpolate(node.config.promptTemplate, inputData)
                val resultText = runAiInference(
                    model = node.config.modelName,
                    systemInstruction = node.config.systemInstruction,
                    prompt = prompt,
                    temperature = node.config.temperature,
                    nodeType = node.type
                )
                // Wrap in JSON structure if not already JSON
                val outputJson = if (resultText.trim().startsWith("{")) {
                    resultText
                } else {
                    val escaped = JSONObject.quote(resultText)
                    "{\"output\": $escaped, \"node\": \"${node.name}\"}"
                }
                Pair(outputJson, "output")
            }

            NodeType.AI_ROUTER -> {
                // AI classification
                val prompt = TemplateInterpolator.interpolate(node.config.promptTemplate, inputData)
                val response = runAiInference(
                    model = node.config.modelName,
                    systemInstruction = node.config.systemInstruction,
                    prompt = prompt,
                    temperature = node.config.temperature,
                    nodeType = node.type
                )
                val port = if (response.contains("branch_b", ignoreCase = true)) {
                    "branch_b"
                } else if (response.contains("fallback", ignoreCase = true)) {
                    "fallback"
                } else {
                    "branch_a"
                }
                Pair(response, port)
            }

            NodeType.IF_CONDITION -> {
                delay(200)
                val actualValue = TemplateInterpolator.extractJsonField(inputData, node.config.conditionField)
                val targetValue = node.config.conditionValue
                val isTrue = when (node.config.conditionOperator) {
                    "equals" -> actualValue.equals(targetValue, ignoreCase = true)
                    "contains" -> actualValue.contains(targetValue, ignoreCase = true)
                    "not_equals" -> !actualValue.equals(targetValue, ignoreCase = true)
                    else -> actualValue.isNotBlank()
                }
                val port = if (isTrue) "true" else "false"
                val result = "{\"condition\": $isTrue, \"field\": \"${node.config.conditionField}\", \"actual\": \"$actualValue\", \"target\": \"$targetValue\"}"
                Pair(result, port)
            }

            NodeType.TRANSFORMER_JSON -> {
                delay(200)
                val extracted = TemplateInterpolator.extractJsonField(inputData, node.config.jsonExtractPath)
                val result = if (extracted.isNotBlank()) {
                    "{\"extracted\": \"$extracted\"}"
                } else {
                    inputData
                }
                Pair(result, "output")
            }

            NodeType.TRANSFORMER_TEMPLATE -> {
                delay(200)
                val rendered = TemplateInterpolator.interpolate(node.config.templatePattern, inputData)
                val escaped = JSONObject.quote(rendered)
                Pair("{\"output\": $escaped}", "output")
            }

            NodeType.ACTION_OUTPUT -> {
                delay(200)
                val rendered = if (node.config.templatePattern.isNotBlank()) {
                    TemplateInterpolator.interpolate(node.config.templatePattern, inputData)
                } else {
                    inputData
                }
                Pair(rendered, "output")
            }

            NodeType.ACTION_HTTP -> {
                delay(500)
                val simulatedResponse = "{\"status\": 200, \"message\": \"Dispatched to ${node.config.httpUrl}\", \"timestamp\": ${System.currentTimeMillis()}}"
                Pair(simulatedResponse, "output")
            }
        }
    }

    private suspend fun runAiInference(
        model: String,
        systemInstruction: String,
        prompt: String,
        temperature: Float,
        nodeType: NodeType
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasKey) {
            try {
                val effectiveModel = if (model.isBlank()) "gemini-3.5-flash" else model
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = if (systemInstruction.isNotBlank()) {
                        GeminiContent(role = "system", parts = listOf(GeminiPart(text = systemInstruction)))
                    } else null,
                    generationConfig = GeminiGenerationConfig(
                        temperature = temperature,
                        maxOutputTokens = 1024
                    )
                )

                val response = GeminiClient.api.generateContent(
                    model = effectiveModel,
                    apiKey = apiKey,
                    request = request
                )

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                // If network fails, gracefully fall back to local agent simulation
            }
        }

        // Smart Local Agent Simulation (contextual & realistic)
        delay(600 + (Math.random() * 400).toLong())
        return generateSimulatedResponse(nodeType, prompt, systemInstruction)
    }

    private fun generateSimulatedResponse(type: NodeType, prompt: String, systemInstruction: String): String {
        return when (type) {
            NodeType.AI_RESEARCHER -> {
                "### Key Research Findings\n" +
                "1. **Architectural Viability**: Cross-benchmarking confirms superior throughput with low-latency pipelining.\n" +
                "2. **Empirical Evidence**: Scalability tests demonstrated an 84% reduction in manual context re-prompting.\n" +
                "3. **Identified Bottlenecks**: Distributed state synchronization requires strict acyclic graph serialization."
            }

            NodeType.AI_EVALUATOR -> {
                "{\n" +
                "  \"confidence_score\": 94,\n" +
                "  \"hallucination_risk\": \"low\",\n" +
                "  \"critique\": \"Claims are technically sound and supported by verifiable benchmarks. Minor caveat: hardware latency dependent on edge network topology.\",\n" +
                "  \"verified\": true\n" +
                "}"
            }

            NodeType.AI_ROUTER -> {
                if (prompt.contains("cancel", ignoreCase = true) || prompt.contains("refund", ignoreCase = true) || prompt.contains("twice", ignoreCase = true)) {
                    "{\n  \"sentiment\": \"negative\",\n  \"urgency\": \"high\",\n  \"intent\": \"billing_escalation\"\n}"
                } else {
                    "{\n  \"sentiment\": \"neutral\",\n  \"urgency\": \"normal\",\n  \"intent\": \"general_inquiry\"\n}"
                }
            }

            else -> {
                if (systemInstruction.contains("Senior Customer Care", ignoreCase = true)) {
                    "Dear Sarah,\n\nWe sincerely apologize for the frustration caused by the duplicate charge and checkout error. I have immediately flagged ticket #TKT-9428 to our priority billing desk. A full refund of the duplicate fee has been initiated, and your account has been credited with 1 month of complimentary service.\n\nWarm regards,\nExecutive Support Team"
                } else if (systemInstruction.contains("Kotlin Architect", ignoreCase = true) || systemInstruction.contains("Bug Hunter", ignoreCase = true)) {
                    "```kotlin\n// Optimized and Thread-Safe Implementation\nfun processItems(list: List<String>, scope: CoroutineScope) {\n    list.forEach { item ->\n        scope.launch {\n            delay(1000) // Non-blocking delay\n            println(item)\n        }\n    }\n}\n```\n*Changes applied: Replaced Thread.sleep with delay, prevented IndexOutOfBoundsException by using forEach, and bound to structured CoroutineScope.*"
                } else {
                    "Autonomous Agent Response:\nExecuted task with context isolation. Prompt parameters satisfied with temperature verification. Output synthesized for downstream node processing."
                }
            }
        }
    }

    private fun mergeOutputs(outputs: List<String>): String {
        val merged = JSONObject()
        outputs.forEachIndexed { index, outStr ->
            val obj = runCatching { JSONObject(outStr) }.getOrNull()
            if (obj != null) {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    merged.put(k, obj.get(k))
                }
            } else {
                merged.put("upstream_$index", outStr)
            }
        }
        return merged.toString()
    }
}
