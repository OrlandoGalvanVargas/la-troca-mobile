package com.example.latroca.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.clickableOnce(
    enabled: Boolean = true,
    debounceTime: Long = 500L, // 500ms de delay
    onClick: () -> Unit
): Modifier = composed {
    var isClickable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope() // 👈 Usar scope del composable

    this.clickable(enabled = enabled && isClickable) {
        if (isClickable) {
            isClickable = false
            onClick()

            scope.launch { // 👈 En vez de GlobalScope
                delay(debounceTime)
                isClickable = true
            }
        }
    }
}