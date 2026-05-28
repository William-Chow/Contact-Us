package com.kotlin.mvvm.contact.view.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ContactOrange = Color(0xFFFF8C00)
private val ContactGray = Color(0xFFDDDDDD)

private val ContactColorScheme = lightColorScheme(
    primary = ContactOrange,
    onPrimary = Color.Black,
    secondary = ContactGray,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun ContactUsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) ContactColorScheme else ContactColorScheme,
        content = content
    )
}
