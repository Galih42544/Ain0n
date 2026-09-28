package com.example.engine

import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode

object DAGResolver {

    /**
     * Checks if the workflow graph contains a cycle.
     */
    fun hasCycle(workflow: Workflow): Boolean {
        val adj = mutableMapOf<String, MutableList<String>>()
        workflow.nodes.forEach { adj[it.id] = mutableListOf() }
        workflow.edges.forEach { edge ->
            adj[edge.fromNodeId]?.add(edge.toNodeId)
        }

        val visited = mutableSetOf<String>()
        val recStack = mutableSetOf<String>()

        fun isCyclicUtil(nodeId: String): Boolean {
            if (recStack.contains(nodeId)) return true
            if (visited.contains(nodeId)) return false

            visited.add(nodeId)
            recStack.add(nodeId)

            val children = adj[nodeId] ?: emptyList()
            for (child in children) {
                if (isCyclicUtil(child)) return true
            }

            recStack.remove(nodeId)
            return false
        }

        for (node in workflow.nodes) {
            if (!visited.contains(node.id)) {
                if (isCyclicUtil(node.id)) return true
            }
        }
        return false
    }

    /**
     * Finds trigger nodes or entry points (nodes with in-degree = 0).
     */
    fun getEntryNodes(workflow: Workflow): List<WorkflowNode> {
        val targetNodeIds = workflow.edges.map { it.toNodeId }.toSet()
        val entryNodes = workflow.nodes.filter { !targetNodeIds.contains(it.id) }
        return if (entryNodes.isNotEmpty()) {
            entryNodes
        } else {
            // Fallback to first node if all have incoming edges
            listOfNotNull(workflow.nodes.firstOrNull())
        }
    }

    /**
     * Finds incoming edges for a node.
     */
    fun getIncomingEdges(workflow: Workflow, nodeId: String): List<WorkflowEdge> {
        return workflow.edges.filter { it.toNodeId == nodeId }
    }

    /**
     * Finds outgoing edges from a node, optionally filtered by port.
     */
    fun getOutgoingEdges(workflow: Workflow, nodeId: String, port: String? = null): List<WorkflowEdge> {
        return workflow.edges.filter {
            it.fromNodeId == nodeId && (port == null || it.fromPort == port)
        }
    }
}
