package com.tasknotes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tasknotes.android.navigation.AppNavigation
import com.tasknotes.android.ui.theme.TaskNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskNotesTheme {
                AppNavigation()
            }
        }
    }
}
