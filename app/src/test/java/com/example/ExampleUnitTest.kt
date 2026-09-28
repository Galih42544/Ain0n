package com.example

import com.example.data.model.NodeType
import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode
import com.example.engine.DAGResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testDAGCycleDetection() {
        val nodeA = WorkflowNode("a", NodeType.TRIGGER_MANUAL, "A", 0f, 0f)
        val nodeB = WorkflowNode("b", NodeType.AI_AGENT, "B", 100f, 0f)
        val nodeC = WorkflowNode("c", NodeType.ACTION_OUTPUT, "C", 200f, 0f)

        // A -> B -> C (Acyclic)
        val edgesAcyclic = listOf(
            WorkflowEdge("1", "a", "output", "b", "input"),
            WorkflowEdge("2", "b", "output", "c", "input")
        )
        val wfAcyclic = Workflow("test", "Acyclic", "", listOf(nodeA, nodeB, nodeC), edgesAcyclic)
        assertFalse(DAGResolver.hasCycle(wfAcyclic))

        // A -> B -> C -> A (Cyclic)
        val edgesCyclic = edgesAcyclic + WorkflowEdge("3", "c", "output", "a", "input")
        val wfCyclic = Workflow("test2", "Cyclic", "", listOf(nodeA, nodeB, nodeC), edgesCyclic)
        assertTrue(DAGResolver.hasCycle(wfCyclic))
    }
}
