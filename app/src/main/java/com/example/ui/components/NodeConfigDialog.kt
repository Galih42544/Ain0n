package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NodeConfig
import com.example.data.model.NodeType
import com.example.data.model.WorkflowNode
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeConfigDialog(
    node: WorkflowNode,
    onDismiss: () -> Unit,
    onSaveConfig: (WorkflowNode) -> Unit,
    onTestNode: suspend (WorkflowNode) -> String
) {
    var name by remember { mutableStateOf(node.name) }
    var promptTemplate by remember { mutableStateOf(node.config.promptTemplate) }
    var systemInstruction by remember { mutableStateOf(node.config.systemInstruction) }
    var modelName by remember { mutableStateOf(node.config.modelName) }
    var temperature by remember { mutableFloatStateOf(node.config.temperature) }
    var initialPayload by remember { mutableStateOf(node.config.initialPayload) }
    var conditionField by remember { mutableStateOf(node.config.conditionField) }
    var conditionOperator by remember { mutableStateOf(node.config.conditionOperator) }
    var conditionValue by remember { mutableStateOf(node.config.conditionValue) }
    var templatePattern by remember { mutableStateOf(node.config.templatePattern) }
    var jsonExtractPath by remember { mutableStateOf(node.config.jsonExtractPath) }
    var httpUrl by remember { mutableStateOf(node.config.httpUrl) }

    var modelMenuExpanded by remember { mutableStateOf(false) }
    val availableModels = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")

    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
            color = Color(0xFF131927)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Configure Node",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = node.type.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = node.type.category.color
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Node Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Node Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_node_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type-Specific Fields
                when (node.type) {
                    NodeType.TRIGGER_MANUAL, NodeType.TRIGGER_WEBHOOK -> {
                        Text("Initial JSON Payload", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = initialPayload,
                            onValueChange = { initialPayload = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("input_initial_payload"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    NodeType.AI_AGENT, NodeType.AI_RESEARCHER, NodeType.AI_EVALUATOR, NodeType.AI_ROUTER -> {
                        // Model Picker
                        ExposedDropdownMenuBox(
                            expanded = modelMenuExpanded,
                            onExpandedChange = { modelMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = modelName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Model") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = FlowPrimary,
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            modelName = model
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // System Role / Persona
                        Text("System Instruction (Persona & Constraints)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = systemInstruction,
                            onValueChange = { systemInstruction = it },
                            placeholder = { Text("e.g. You are an expert scientific fact-checker.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("input_system_instruction"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Prompt Template
                        Text("User Prompt Template", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(
                            text = "Reference upstream variables using {{expression}}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Variable Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("{{\${'$'}json.output}}", "{{\${'$'}json.topic}}", "{{\${'$'}json.message}}", "{{input}}").forEach { token ->
                                FilterChip(
                                    selected = false,
                                    onClick = { promptTemplate += " $token" },
                                    label = { Text(token, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF1E2638),
                                        labelColor = FlowSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = promptTemplate,
                            onValueChange = { promptTemplate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("input_prompt_template"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Temperature Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temperature (Creativity):", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(String.format("%.2f", temperature), color = FlowSecondary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = FlowSecondary,
                                activeTrackColor = FlowPrimary
                            )
                        )
                    }

                    NodeType.IF_CONDITION -> {
                        Text("Conditional Routing Expression", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = conditionField,
                            onValueChange = { conditionField = it },
                            label = { Text("Field Path (e.g. sentiment)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = conditionOperator,
                            onValueChange = { conditionOperator = it },
                            label = { Text("Operator (equals | contains | not_equals)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = conditionValue,
                            onValueChange = { conditionValue = it },
                            label = { Text("Target Match Value (e.g. negative)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    NodeType.TRANSFORMER_JSON -> {
                        Text("Extract Field Path", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = jsonExtractPath,
                            onValueChange = { jsonExtractPath = it },
                            label = { Text("JSON Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    NodeType.TRANSFORMER_TEMPLATE, NodeType.ACTION_OUTPUT -> {
                        Text("Template Pattern", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = templatePattern,
                            onValueChange = { templatePattern = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )
                    }

                    NodeType.ACTION_HTTP -> {
                        Text("HTTP Destination", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = httpUrl,
                            onValueChange = { httpUrl = it },
                            label = { Text("Webhook / REST URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Node Individually Button & Live Result
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Node Sandbox Test",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        isTesting = true
                                        testResult = null
                                        val testNode = node.copy(
                                            name = name,
                                            config = node.config.copy(
                                                promptTemplate = promptTemplate,
                                                systemInstruction = systemInstruction,
                                                modelName = modelName,
                                                temperature = temperature,
                                                initialPayload = initialPayload,
                                                conditionField = conditionField,
                                                conditionOperator = conditionOperator,
                                                conditionValue = conditionValue,
                                                templatePattern = templatePattern,
                                                jsonExtractPath = jsonExtractPath,
                                                httpUrl = httpUrl
                                            )
                                        )
                                        testResult = onTestNode(testNode)
                                        isTesting = false
                                    }
                                },
                                enabled = !isTesting,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                modifier = Modifier.testTag("btn_test_single_node")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = FlowSecondary)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = FlowSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run Step", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }

                        if (testResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF090D16))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = testResult ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    color = Color(0xFFA5F3FC)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val updated = node.copy(
                                name = name,
                                config = node.config.copy(
                                    promptTemplate = promptTemplate,
                                    systemInstruction = systemInstruction,
                                    modelName = modelName,
                                    temperature = temperature,
                                    initialPayload = initialPayload,
                                    conditionField = conditionField,
                                    conditionOperator = conditionOperator,
                                    conditionValue = conditionValue,
                                    templatePattern = templatePattern,
                                    jsonExtractPath = jsonExtractPath,
                                    httpUrl = httpUrl
                                )
                            )
                            onSaveConfig(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                        modifier = Modifier.testTag("btn_save_node_config")
                    ) {
                        Text("Save Configuration", color = Color.White)
                    }
                }
            }
        }
    }
}
