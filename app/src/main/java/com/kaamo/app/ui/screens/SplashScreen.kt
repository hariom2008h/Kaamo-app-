package com.kaamo.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaamo.app.ui.components.KaamoFullLogo
import com.kaamo.app.ui.theme.KaamoColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        visible = true
        delay(1600)
        finished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(KaamoColors.White),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(700)) + scaleIn(initialScale = 0.92f, animationSpec = tween(700)),
        ) {
            KaamoFullLogo(size = 240.dp)
        }
    }
}
