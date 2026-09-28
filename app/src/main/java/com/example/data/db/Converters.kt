package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.NodeConfig
import com.example.data.model.NodeType
import com.example.data.model.WorkflowEdge
import com.example.data.model.WorkflowNode
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromNodeList(nodes: List<WorkflowNode>?): String {
        if (nodes == null) return "[]"
        val array = JSONArray()
        for (node in nodes) {
            val obj = JSONObject()
            obj.put("id", node.id)
            obj.put("type", node.type.name)
            obj.put("name", node.name)
            obj.put("positionX", node.positionX.toDouble())
            obj.put("positionY", node.positionY.toDouble())

            val cfg = JSONObject()
            cfg.put("promptTemplate", node.config.promptTemplate)
            cfg.put("systemInstruction", node.config.systemInstruction)
            cfg.put("modelName", node.config.modelName)
            cfg.put("temperature", node.config.temperature.toDouble())
            cfg.put("initialPayload", node.config.initialPayload)
            cfg.put("conditionField", node.config.conditionField)
            cfg.put("conditionOperator", node.config.conditionOperator)
            cfg.put("conditionValue", node.config.conditionValue)
            cfg.put("templatePattern", node.config.templatePattern)
            cfg.put("jsonExtractPath", node.config.jsonExtractPath)
            cfg.put("httpUrl", node.config.httpUrl)
            cfg.put("httpMethod", node.config.httpMethod)
            cfg.put("retryCount", node.config.retryCount)
            cfg.put("isSimulationFallbackEnabled", node.config.isSimulationFallbackEnabled)

            obj.put("config", cfg)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toNodeList(jsonStr: String?): List<WorkflowNode> {
        if (jsonStr.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<WorkflowNode>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val typeName = obj.optString("type", NodeType.AI_AGENT.name)
                val type = runCatching { NodeType.valueOf(typeName) }.getOrDefault(NodeType.AI_AGENT)
                val name = obj.optString("name", "Node")
                val posX = obj.optDouble("positionX", 0.0).toFloat()
                val posY = obj.optDouble("positionY", 0.0).toFloat()

                val cfgObj = obj.optJSONObject("config")
                val config = if (cfgObj != null) {
                    NodeConfig(
                        promptTemplate = cfgObj.optString("promptTemplate", ""),
                        systemInstruction = cfgObj.optString("systemInstruction", "You are an AI Agent."),
                        modelName = cfgObj.optString("modelName", "gemini-3.5-flash"),
                        temperature = cfgObj.optDouble("temperature", 0.7).toFloat(),
                        initialPayload = cfgObj.optString("initialPayload", "{\"query\":\"test\"}"),
                        conditionField = cfgObj.optString("conditionField", "sentiment"),
                        conditionOperator = cfgObj.optString("conditionOperator", "equals"),
                        conditionValue = cfgObj.optString("conditionValue", "positive"),
                        templatePattern = cfgObj.optString("templatePattern", "{{\${'$'}json.output}}"),
                        jsonExtractPath = cfgObj.optString("jsonExtractPath", "output"),
                        httpUrl = cfgObj.optString("httpUrl", "https://api.example.com"),
                        httpMethod = cfgObj.optString("httpMethod", "POST"),
                        retryCount = cfgObj.optInt("retryCount", 1),
                        isSimulationFallbackEnabled = cfgObj.optBoolean("isSimulationFallbackEnabled", true)
                    )
                } else {
                    NodeConfig()
                }

                list.add(WorkflowNode(id = id, type = type, name = name, positionX = posX, positionY = posY, config = config))
            }
        } catch (_: Exception) {
        }
        return list
    }

    @TypeConverter
    fun fromEdgeList(edges: List<WorkflowEdge>?): String {
        if (edges == null) return "[]"
        val array = JSONArray()
        for (edge in edges) {
            val obj = JSONObject()
            obj.put("id", edge.id)
            obj.put("fromNodeId", edge.fromNodeId)
            obj.put("fromPort", edge.fromPort)
            obj.put("toNodeId", edge.toNodeId)
            obj.put("toPort", edge.toPort)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toEdgeList(jsonStr: String?): List<WorkflowEdge> {
        if (jsonStr.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<WorkflowEdge>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val fromNodeId = obj.optString("fromNodeId", "")
                val fromPort = obj.optString("fromPort", "output")
                val toNodeId = obj.optString("toNodeId", "")
                val toPort = obj.optString("toPort", "input")
                list.add(WorkflowEdge(id = id, fromNodeId = fromNodeId, fromPort = fromPort, toNodeId = toNodeId, toPort = toPort))
            }
        } catch (_: Exception) {
        }
        return list
    }
}
