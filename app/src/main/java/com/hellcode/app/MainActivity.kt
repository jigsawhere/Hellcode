package com.hellcode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hellcode.app.ai.AIManager
import com.hellcode.app.ui.screens.AIChatScreen
import com.hellcode.app.ui.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val aiManager = AIManager(HellApp.instance)

    NavHost(navController = navController, startDestination = "ai_chat_screen") {
        composable("ai_chat_screen") {
            AIChatScreen(aiManager = aiManager)
        }
        // TODO: Add other routes for "editor_screen", "terminal_screen", etc.
    }
}