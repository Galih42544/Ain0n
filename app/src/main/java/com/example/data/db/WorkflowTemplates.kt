package com.example.data.db

import com.example.data.model.NodeConfig
import com.example.data.model.NodeType
import com.example.data.model.Workflow
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode
import java.util.UUID

object WorkflowTemplates {

    fun getPreloadedWorkflows(): List<Workflow> {
        return listOf(
            createResearchAndFactCheckWorkflow(),
            createSupportTriageWorkflow(),
            createCodeReviewerWorkflow(),
            createContentStudioWorkflow()
        )
    }

    private fun createResearchAndFactCheckWorkflow(): Workflow {
        val wfId = "template-research-evaluator"
        val nTrigger = WorkflowNode(
            id = "node-1-trigger",
            type = NodeType.TRIGGER_MANUAL,
            name = "Research Topic Input",
            positionX = 60f,
            positionY = 180f,
            config = NodeConfig(
                initialPayload = "{\"topic\": \"Breakthroughs in Quantum Error Correction and practical qubit scaling for 2026\"}"
            )
        )

        val nPlanner = WorkflowNode(
            id = "node-2-planner",
            type = NodeType.AI_AGENT,
            name = "Research Planner Agent",
            positionX = 380f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a Research Director. Deconstruct the given topic into 3 specific technological pillars, hypothesis questions, and critical metrics to evaluate.",
                promptTemplate = "Topic to investigate:\n{{\${'$'}json.topic}}\n\nProvide a structured breakdown of what to investigate.",
                temperature = 0.5f
            )
        )

        val nResearcher = WorkflowNode(
            id = "node-3-researcher",
            type = NodeType.AI_RESEARCHER,
            name = "Deep Evidence Gatherer",
            positionX = 720f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are an expert Scientific Researcher. Provide concrete technical findings, methodologies, and architectural details based on the research plan.",
                promptTemplate = "Investigate the following research plan and produce detailed technical findings with trade-offs:\n\n{{\${'$'}json.output}}",
                temperature = 0.6f
            )
        )

        val nEvaluator = WorkflowNode(
            id = "node-4-evaluator",
            type = NodeType.AI_EVALUATOR,
            name = "Fact-Check & Critic Agent",
            positionX = 1060f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.1-pro-preview",
                systemInstruction = "You are a ruthless Peer Reviewer and Fact-Checker. Check the research claims for hype, potential physical constraints, and assign a Confidence Score (0-100%).",
                promptTemplate = "Critique and verify this research report. Highlight any assumptions or unproven extrapolations:\n\n{{\${'$'}json.output}}",
                temperature = 0.2f
            )
        )

        val nFinalOutput = WorkflowNode(
            id = "node-5-output",
            type = NodeType.ACTION_OUTPUT,
            name = "Executive Synthesis",
            positionX = 1400f,
            positionY = 180f,
            config = NodeConfig(
                templatePattern = "=== EXECUTIVE BRIEFING & CONFIDENCE SCORE ===\n{{\${'$'}json.output}}"
            )
        )

        val edges = listOf(
            WorkflowEdge(UUID.randomUUID().toString(), nTrigger.id, "output", nPlanner.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nPlanner.id, "output", nResearcher.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nResearcher.id, "output", nEvaluator.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nEvaluator.id, "output", nFinalOutput.id, "input")
        )

        return Workflow(
            id = wfId,
            name = "Multi-Agent Research & Fact-Checker",
            description = "Orchestrates 3 distinct AI agents (Planner -> Researcher -> Critic/Fact-Checker) to eliminate hallucinations and produce verified executive reports.",
            nodes = listOf(nTrigger, nPlanner, nResearcher, nEvaluator, nFinalOutput),
            edges = edges,
            isTemplate = true,
            category = "Research"
        )
    }

    private fun createSupportTriageWorkflow(): Workflow {
        val wfId = "template-support-triage"
        val nTrigger = WorkflowNode(
            id = "triage-1-trigger",
            type = NodeType.TRIGGER_WEBHOOK,
            name = "Customer Ticket Ingest",
            positionX = 60f,
            positionY = 220f,
            config = NodeConfig(
                initialPayload = "{\"ticketId\": \"TKT-9428\", \"customer\": \"Sarah Jenkins\", \"message\": \"I was charged twice for my subscription renewals and your app crashed during checkout! Fix this or cancel my account!\"}"
            )
        )

        val nClassifier = WorkflowNode(
            id = "triage-2-classifier",
            type = NodeType.AI_ROUTER,
            name = "Sentiment & Intent Classifier",
            positionX = 380f,
            positionY = 200f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a Support Triage AI. Analyze the customer message. You MUST return JSON with: sentiment (positive|negative|neutral), urgency (high|normal), and intent (billing|bug|general).",
                promptTemplate = "Analyze this customer ticket:\n{{\${'$'}json.message}}\n\nOutput only JSON.",
                temperature = 0.1f
            )
        )

        val nCondition = WorkflowNode(
            id = "triage-3-condition",
            type = NodeType.IF_CONDITION,
            name = "Is Negative Sentiment?",
            positionX = 720f,
            positionY = 200f,
            config = NodeConfig(
                conditionField = "sentiment",
                conditionOperator = "equals",
                conditionValue = "negative"
            )
        )

        val nEscalationAgent = WorkflowNode(
            id = "triage-4-escalation",
            type = NodeType.AI_AGENT,
            name = "Crisis Support Specialist",
            positionX = 1060f,
            positionY = 90f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a Senior Customer Care Specialist. Craft a deeply empathetic, reassuring, immediate response with an apology, refund priority assurance, and direct escalation.",
                promptTemplate = "Write a high-priority empathetic response for:\n{{\${'$'}json.output}}",
                temperature = 0.6f
            )
        )

        val nStandardAgent = WorkflowNode(
            id = "triage-5-standard",
            type = NodeType.AI_AGENT,
            name = "Standard FAQ Agent",
            positionX = 1060f,
            positionY = 320f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a friendly Customer Support assistant. Provide concise, helpful instructions.",
                promptTemplate = "Provide standard guidance for:\n{{\${'$'}json.output}}",
                temperature = 0.5f
            )
        )

        val nAction = WorkflowNode(
            id = "triage-6-action",
            type = NodeType.ACTION_HTTP,
            name = "Dispatch to CRM Webhook",
            positionX = 1420f,
            positionY = 200f,
            config = NodeConfig(
                httpUrl = "https://api.crm.internal/v1/tickets/auto-reply",
                httpMethod = "POST"
            )
        )

        val edges = listOf(
            WorkflowEdge(UUID.randomUUID().toString(), nTrigger.id, "output", nClassifier.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nClassifier.id, "output", nCondition.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nCondition.id, "true", nEscalationAgent.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nCondition.id, "false", nStandardAgent.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nEscalationAgent.id, "output", nAction.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nStandardAgent.id, "output", nAction.id, "input")
        )

        return Workflow(
            id = wfId,
            name = "Customer Support Triage & Auto-Responder",
            description = "Autonomous ticket classification and conditional routing based on customer sentiment and urgency.",
            nodes = listOf(nTrigger, nClassifier, nCondition, nEscalationAgent, nStandardAgent, nAction),
            edges = edges,
            isTemplate = true,
            category = "Customer Care"
        )
    }

    private fun createCodeReviewerWorkflow(): Workflow {
        val wfId = "template-code-reviewer"
        val nTrigger = WorkflowNode(
            id = "code-1-trigger",
            type = NodeType.TRIGGER_MANUAL,
            name = "Code Patch Input",
            positionX = 60f,
            positionY = 180f,
            config = NodeConfig(
                initialPayload = "{\"code\": \"fun processItems(list: List<String>) { for (i in 0..list.size) { val item = list[i]; launch { Thread.sleep(1000); println(item) } } }\"}"
            )
        )

        val nAnalyzer = WorkflowNode(
            id = "code-2-analyzer",
            type = NodeType.AI_AGENT,
            name = "Static Bug Hunter Agent",
            positionX = 390f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a Principal Software Engineer. Identify index bounds errors, blocking calls in coroutines, memory leaks, and performance hazards in this snippet.",
                promptTemplate = "Review this code snippet:\n```kotlin\n{{\${'$'}json.code}}\n```\nList all architectural and runtime flaws found.",
                temperature = 0.2f
            )
        )

        val nFixer = WorkflowNode(
            id = "code-3-fixer",
            type = NodeType.AI_AGENT,
            name = "Refactor & Code Fixer",
            positionX = 740f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.1-pro-preview",
                systemInstruction = "You are an expert Kotlin Architect. Produce the production-ready corrected code with best practices (e.g. delay instead of Thread.sleep, indices loop, structured concurrency).",
                promptTemplate = "Based on this bug analysis:\n{{\${'$'}json.output}}\n\nWrite the clean, optimized, production-ready replacement code.",
                temperature = 0.3f
            )
        )

        val nOutput = WorkflowNode(
            id = "code-4-output",
            type = NodeType.ACTION_OUTPUT,
            name = "Verified Code Artifact",
            positionX = 1090f,
            positionY = 180f,
            config = NodeConfig(
                templatePattern = "=== REFACTORED CODE ===\n{{\${'$'}json.output}}"
            )
        )

        val edges = listOf(
            WorkflowEdge(UUID.randomUUID().toString(), nTrigger.id, "output", nAnalyzer.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nAnalyzer.id, "output", nFixer.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nFixer.id, "output", nOutput.id, "input")
        )

        return Workflow(
            id = wfId,
            name = "Code Reviewer & Auto-Fixer Pipeline",
            description = "Analyzes source code for runtime bugs, edge cases, and off-by-one errors, then automatically generates corrected idiomatic code.",
            nodes = listOf(nTrigger, nAnalyzer, nFixer, nOutput),
            edges = edges,
            isTemplate = true,
            category = "Engineering"
        )
    }

    private fun createContentStudioWorkflow(): Workflow {
        val wfId = "template-content-studio"
        val nTrigger = WorkflowNode(
            id = "content-1-trigger",
            type = NodeType.TRIGGER_MANUAL,
            name = "Topic Brief",
            positionX = 60f,
            positionY = 180f,
            config = NodeConfig(
                initialPayload = "{\"topic\": \"How Autonomous AI Agents are replacing traditional scripts in 2026\"}"
            )
        )

        val nArchitect = WorkflowNode(
            id = "content-2-architect",
            type = NodeType.AI_AGENT,
            name = "Story Arc & Hook Generator",
            positionX = 380f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are a master Content Strategist. Create a compelling narrative outline with an irresistible hook, 3 counter-intuitive insights, and a punchy closing.",
                promptTemplate = "Craft a story structure for this topic:\n{{\${'$'}json.topic}}",
                temperature = 0.7f
            )
        )

        val nWriter = WorkflowNode(
            id = "content-3-writer",
            type = NodeType.AI_AGENT,
            name = "Deep Article Writer",
            positionX = 720f,
            positionY = 160f,
            config = NodeConfig(
                modelName = "gemini-3.5-flash",
                systemInstruction = "You are an engaging Tech Essayist. Write an informative, crisp 400-word article following the provided story outline.",
                promptTemplate = "Write the article based on this outline:\n{{\${'$'}json.output}}",
                temperature = 0.7f
            )
        )

        val nTransformer = WorkflowNode(
            id = "content-4-transformer",
            type = NodeType.TRANSFORMER_TEMPLATE,
            name = "Multi-Channel Formatter",
            positionX = 1060f,
            positionY = 160f,
            config = NodeConfig(
                templatePattern = "🚀 **ARTICLE PUBLICATION DRAFT**\n\n{{\${'$'}json.output}}\n\n---\n*Generated by NodeFlow Multi-Agent Pipeline*"
            )
        )

        val nOutput = WorkflowNode(
            id = "content-5-output",
            type = NodeType.ACTION_OUTPUT,
            name = "Published Content",
            positionX = 1400f,
            positionY = 180f,
            config = NodeConfig()
        )

        val edges = listOf(
            WorkflowEdge(UUID.randomUUID().toString(), nTrigger.id, "output", nArchitect.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nArchitect.id, "output", nWriter.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nWriter.id, "output", nTransformer.id, "input"),
            WorkflowEdge(UUID.randomUUID().toString(), nTransformer.id, "output", nOutput.id, "input")
        )

        return Workflow(
            id = wfId,
            name = "Multi-Step Content Creator",
            description = "Transforms a single prompt into an outline, full draft, and formatted cross-platform publication.",
            nodes = listOf(nTrigger, nArchitect, nWriter, nTransformer, nOutput),
            edges = edges,
            isTemplate = true,
            category = "Marketing"
        )
    }
}
