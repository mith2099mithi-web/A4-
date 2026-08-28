package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.editor.EditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var activeDocumentId by remember { mutableStateOf<String?>(null) }

                if (activeDocumentId == null) {
                    HomeScreen(
                        onOpenDocument = { docId ->
                            activeDocumentId = docId
                        }
                    )
                } else {
                    BackHandler {
                        activeDocumentId = null
                    }
                    EditorScreen(
                        documentId = activeDocumentId!!,
                        onNavigateBack = {
                            activeDocumentId = null
                        }
                    )
                }
            }
        }
    }
}
