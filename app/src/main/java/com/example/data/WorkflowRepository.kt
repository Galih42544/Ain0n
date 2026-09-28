package com.example.data

import com.example.data.db.ExecutionHistoryEntity
import com.example.data.db.WorkflowDao
import com.example.data.db.WorkflowEntity
import com.example.data.db.WorkflowTemplates
import com.example.data.model.Workflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkflowRepository(private val dao: WorkflowDao) {

    val allWorkflows: Flow<List<Workflow>> = dao.getAllWorkflows().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentExecutions: Flow<List<ExecutionHistoryEntity>> = dao.getRecentExecutions()

    suspend fun getWorkflowById(id: String): Workflow? {
        return dao.getWorkflowById(id)?.toDomain()
    }

    suspend fun saveWorkflow(workflow: Workflow) {
        dao.insertWorkflow(WorkflowEntity.fromDomain(workflow))
    }

    suspend fun deleteWorkflow(id: String) {
        dao.deleteWorkflowById(id)
    }

    suspend fun saveExecution(execution: ExecutionHistoryEntity) {
        dao.insertExecution(execution)
    }

    suspend fun seedTemplatesIfEmpty() {
        val templates = WorkflowTemplates.getPreloadedWorkflows()
        val entities = templates.map { WorkflowEntity.fromDomain(it) }
        dao.insertAll(entities)
    }
}
