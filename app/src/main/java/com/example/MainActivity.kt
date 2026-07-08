package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.LunaCareApp
import com.example.ui.theme.LunaCareTheme
import com.example.viewmodel.LunaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LunaViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LunaViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract deep link path for Google App Indexing and SEO mapping
        intent?.data?.path?.let { path ->
            viewModel.handleDeepLink(path)
        }

        enableEdgeToEdge()
        setContent {
            val profileState by viewModel.profile.collectAsState()
            val isDarkTheme = profileState?.isDarkMode ?: false

            LunaCareTheme(darkTheme = isDarkTheme) {
                LunaCareApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.path?.let { path ->
            viewModel.handleDeepLink(path)
        }
    }
}
