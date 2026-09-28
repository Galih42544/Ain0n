package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WorkflowRepository
import com.example.data.db.AppDatabase
import com.example.ui.screens.WorkflowEditorScreen
import com.example.ui.screens.WorkflowListScreen
import com.example.ui.theme.NodeFlowTheme
import com.example.ui.viewmodel.WorkflowViewModel
import com.example.ui.viewmodel.WorkflowViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: WorkflowViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WorkflowRepository(database.workflowDao())
        WorkflowViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NodeFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D16)
                ) {
                    NodeFlowApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NodeFlowApp(viewModel: WorkflowViewModel) {
    val currentWorkflow by viewModel.currentWorkflow.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentWorkflow,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_nav"
    ) { workflow ->
        if (workflow == null) {
            WorkflowListScreen(
                viewModel = viewModel,
                onWorkflowSelected = { selected ->
                    viewModel.selectWorkflow(selected)
                }
            )
        } else {
            WorkflowEditorScreen(
                workflow = workflow,
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.selectWorkflowById("") // clears currentWorkflow
                }
            )
        }
    }
}
