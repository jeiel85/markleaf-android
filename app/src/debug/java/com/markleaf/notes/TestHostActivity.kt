package com.markleaf.notes

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

class TestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            content?.invoke()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            content = null
        }
    }

    companion object {
        var content: (@Composable () -> Unit)? = null
    }
}
