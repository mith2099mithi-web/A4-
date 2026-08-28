package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
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

                // Smooth screen transition: the editor slides in from the
                // right when a document opens, and slides back out on back.
                AnimatedContent(
                    targetState = activeDocumentId,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } +
                                fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(240, easing = FastOutLinearInEasing)) { -it / 4 } +
                                    fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 4 } +
                                fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it } +
                                    fadeOut(tween(160)))
                        }
                    },
                    label = "screenTransition"
                ) { docId ->
                    if (docId == null) {
                        HomeScreen(
                            onOpenDocument = { newDocId ->
                                activeDocumentId = newDocId
                            }
                        )
                    } else {
                        EditorScreen(
                            documentId = docId,
                            onNavigateBack = {
                                activeDocumentId = null
                            }
                        )
                    }
                }
            }
        }
    }
}
