package com.example.data.model

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class NodeStepLog(
    val nodeId: String,
    val nodeName: String,
    val nodeType: NodeType,
    val status: ExecutionStatus,
    val inputData: String,
    val outputData: String = "",
    val errorMessage: String? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationMs: Long = 0L,
    val modelUsed: String? = null
)

data class WorkflowExecutionState(
    val executionId: String = "",
    val workflowId: String = "",
    val overallStatus: ExecutionStatus = ExecutionStatus.IDLE,
    val activeNodeIds: Set<String> = emptySet(),
    val completedNodeIds: Set<String> = emptySet(),
    val failedNodeIds: Set<String> = emptySet(),
    val stepLogs: Map<String, NodeStepLog> = emptyMap(),
    val startedAt: Long = 0L,
    val completedAt: Long = 0L,
    val totalDurationMs: Long = 0L,
    val summaryError: String? = null
)
