package com.madhav0637.budgetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madhav0637.budgetapp.ui.RootScreen
import com.madhav0637.budgetapp.ui.theme.KokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BudgetApplication
        // Debug builds can be launched with sample data for screenshots; release builds always get the defaults.
        // This must run before anything reads the database or settings.
        val launch = if (savedInstanceState == null) LaunchOptions.apply(app, intent) else LaunchRequest()
        enableEdgeToEdge()
        setContent {
            val appearance by app.settings.appearance.collectAsStateWithLifecycle()
            val highlight by app.settings.highlight.collectAsStateWithLifecycle()
            KokuTheme(appearance, highlight) {
                RootScreen(launch)
            }
        }
    }
}
