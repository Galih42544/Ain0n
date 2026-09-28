package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowSecondary
import com.example.ui.theme.FlowSuccess
import com.example.ui.theme.FlowTertiary

@Composable
fun ArchitectureGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                            text = "Arsitektur n8n AI Engine",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "4 Pilar Workflow & Multi-Agent Orchestration",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlowSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_guide_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GuidePillarCard(
                    number = "1",
                    title = "Sistem Manajemen State Workflow",
                    icon = Icons.Default.Memory,
                    iconColor = FlowSuccess,
                    description = "Menstandarisasi aliran data antar node menggunakan JSON Schema dan expression parsing (misal: {{\${'$'}json.output}}). Memastikan output dari Node A dapat dipetakan secara aman ke Node B."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuidePillarCard(
                    number = "2",
                    title = "Visual Node Graph (DAG Canvas)",
                    icon = Icons.Default.Hub,
                    iconColor = FlowSecondary,
                    description = "Kanvas interaktif drag-and-drop dengan kurva Bezier. Mendukung zoom, pan, dan linking antar input/output port untuk membangun alur kerja visual tanpa coding manual."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuidePillarCard(
                    number = "3",
                    title = "Workflow Execution Engine",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = FlowPrimary,
                    description = "Otak eksekusi berbasis Directed Acyclic Graph (DAG). Melakukan topological sort, traversal paralel atau sekuensial, conditional branching (If/Else), serta error handling & retry otomatis."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuidePillarCard(
                    number = "4",
                    title = "Isolasi Konteks & Keamanan AI",
                    icon = Icons.Default.Security,
                    iconColor = FlowTertiary,
                    description = "Memisahkan System Prompt, Temperature, dan Memory Context pada setiap Agen AI agar peran Agen Peneliti (Researcher) tidak bercampur atau mencemari Agen Pengkritik (Evaluator)."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 NodeFlow mengimplementasikan seluruh 4 pilar ini secara lokal di Android dengan Room Database, Kotlin Coroutines Flow, dan REST Gemini AI API!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5F3FC)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidePillarCard(
    number: String,
    title: String,
    icon: ImageVector,
    iconColor: Color,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Pilar $number: $title",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
