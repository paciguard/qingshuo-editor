package com.qingshuo.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.qingshuo.editor.ui.screens.EditorScreen
import com.qingshuo.editor.ui.screens.HomeScreen
import com.qingshuo.editor.ui.theme.QingshuoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QingshuoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    when (val s = screen) {
                        is Screen.Home -> HomeScreen(
                            onOpenEditor = { screen = Screen.Editor }
                        )
                        is Screen.Editor -> EditorScreen(
                            onExit = { screen = Screen.Home }
                        )
                    }
                }
            }
        }
    }

    sealed interface Screen {
        data object Home : Screen
        data object Editor : Screen
    }
}
