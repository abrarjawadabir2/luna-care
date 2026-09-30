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
    private val viewModel: LunaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val profileState by viewModel.profile.collectAsState()
            val isDarkTheme = profileState?.isDarkMode ?: false

            LunaCareTheme(darkTheme = isDarkTheme) {
                LunaCareApp(viewModel = viewModel)
            }
        }
    }
}
