package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class NodeCategory(val title: String, val color: Color) {
    TRIGGER("Trigger", Color(0xFF10B981)), // Emerald
    AI_AGENT("AI Agent", Color(0xFF8B5CF6)), // Purple/Violet
    LOGIC("Logic & Routing", Color(0xFFF59E0B)), // Amber
    TRANSFORM("Data & Transform", Color(0xFF06B6D4)), // Cyan
    ACTION("Output & Action", Color(0xFFF43F5E)) // Rose
}

enum class NodeType(
    val title: String,
    val category: NodeCategory,
    val description: String,
    val defaultIconName: String,
    val outputPorts: List<String> = listOf("output"),
    val inputPorts: List<String> = listOf("input")
) {
    TRIGGER_MANUAL(
        title = "Manual Trigger",
        category = NodeCategory.TRIGGER,
        description = "Starts workflow execution with user text or JSON input payload.",
        defaultIconName = "PlayArrow",
        outputPorts = listOf("output"),
        inputPorts = emptyList()
    ),
    TRIGGER_WEBHOOK(
        title = "Webhook Trigger",
        category = NodeCategory.TRIGGER,
        description = "Simulates incoming HTTP webhook payload or external event.",
        defaultIconName = "Webhook",
        outputPorts = listOf("output"),
        inputPorts = emptyList()
    ),
    AI_AGENT(
        title = "AI Agent (Gemini)",
        category = NodeCategory.AI_AGENT,
        description = "Configurable LLM Agent with isolated system role, temperature, and prompt template.",
        defaultIconName = "SmartToy",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    ),
    AI_RESEARCHER(
        title = "AI Researcher",
        category = NodeCategory.AI_AGENT,
        description = "Specialized agent configured to break down complex queries and extract structured facts.",
        defaultIconName = "Psychology",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    ),
    AI_EVALUATOR(
        title = "AI Evaluator / Fact Checker",
        category = NodeCategory.AI_AGENT,
        description = "Evaluates quality, detects hallucinations, and outputs confidence scores.",
        defaultIconName = "FactCheck",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    ),
    AI_ROUTER(
        title = "AI Classifier / Router",
        category = NodeCategory.LOGIC,
        description = "Categorizes input and branches into conditional execution paths.",
        defaultIconName = "AltRoute",
        outputPorts = listOf("branch_a", "branch_b", "fallback"),
        inputPorts = listOf("input")
    ),
    IF_CONDITION(
        title = "If / Else Condition",
        category = NodeCategory.LOGIC,
        description = "Evaluates conditions on input variables (e.g. sentiment == 'positive').",
        defaultIconName = "CallSplit",
        outputPorts = listOf("true", "false"),
        inputPorts = listOf("input")
    ),
    TRANSFORMER_JSON(
        title = "JSON Extractor & Formatter",
        category = NodeCategory.TRANSFORM,
        description = "Extracts fields, parses raw responses, or reshapes JSON structures.",
        defaultIconName = "DataObject",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    ),
    TRANSFORMER_TEMPLATE(
        title = "Text Template Builder",
        category = NodeCategory.TRANSFORM,
        description = "Interpolates multiple upstream outputs into a cohesive document or email.",
        defaultIconName = "Article",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    ),
    ACTION_OUTPUT(
        title = "Workflow Result",
        category = NodeCategory.ACTION,
        description = "Collects, highlights, and displays final workflow execution results.",
        defaultIconName = "DoneAll",
        outputPorts = emptyList(),
        inputPorts = listOf("input")
    ),
    ACTION_HTTP(
        title = "HTTP Request",
        category = NodeCategory.ACTION,
        description = "Dispatches output payload to an external REST endpoint or simulated webhook.",
        defaultIconName = "Send",
        outputPorts = listOf("output"),
        inputPorts = listOf("input")
    )
}
