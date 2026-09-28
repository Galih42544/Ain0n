package com.example.data.model

data class NodeConfig(
    val promptTemplate: String = "",
    val systemInstruction: String = "You are an autonomous AI Agent in a workflow automation pipeline.",
    val modelName: String = "gemini-3.5-flash",
    val temperature: Float = 0.7f,
    val initialPayload: String = "{\"query\": \"Summarize latest tech advancements\"}",
    val conditionField: String = "sentiment",
    val conditionOperator: String = "equals",
    val conditionValue: String = "positive",
    val templatePattern: String = "Report:\n{{\${'$'}json.output}}",
    val jsonExtractPath: String = "output",
    val httpUrl: String = "https://api.example.com/webhook",
    val httpMethod: String = "POST",
    val retryCount: Int = 1,
    val isSimulationFallbackEnabled: Boolean = true
)

data class WorkflowNode(
    val id: String,
    val type: NodeType,
    val name: String,
    val positionX: Float,
    val positionY: Float,
    val config: NodeConfig = NodeConfig()
)

data class WorkflowEdge(
    val id: String,
    val fromNodeId: String,
    val fromPort: String = "output",
    val toNodeId: String,
    val toPort: String = "input"
)

data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val nodes: List<WorkflowNode> = emptyList(),
    val edges: List<WorkflowEdge> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isTemplate: Boolean = false,
    val category: String = "General"
)
