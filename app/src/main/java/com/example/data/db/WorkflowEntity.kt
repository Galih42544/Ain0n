package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val nodes: List<WorkflowNode>,
    val edges: List<WorkflowEdge>,
    val updatedAt: Long,
    val isTemplate: Boolean,
    val category: String
) {
    fun toDomain(): Workflow {
        return Workflow(
            id = id,
            name = name,
            description = description,
            nodes = nodes,
            edges = edges,
            updatedAt = updatedAt,
            isTemplate = isTemplate,
            category = category
        )
    }

    companion object {
        fun fromDomain(domain: Workflow): WorkflowEntity {
            return WorkflowEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                nodes = domain.nodes,
                edges = domain.edges,
                updatedAt = domain.updatedAt,
                isTemplate = domain.isTemplate,
                category = domain.category
            )
        }
    }
}

@Entity(tableName = "execution_history")
data class ExecutionHistoryEntity(
    @PrimaryKey val id: String,
    val workflowId: String,
    val workflowName: String,
    val status: String,
    val totalDurationMs: Long,
    val startedAt: Long,
    val completedAt: Long,
    val stepLogsJson: String,
    val summaryError: String?
)
