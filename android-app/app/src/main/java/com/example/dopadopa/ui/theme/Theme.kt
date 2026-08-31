package com.example.dopadopa.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * style.css の配色トークン（iOS 版 Theme.swift）を Kotlin 側に移植したもの。
 */
object Theme {
    val primary = Color(0xFF3D7BFF) // #3d7bff
    val accent = Color(0xFF1AC29A) // #1ac29a
    val ink = Color(0xFF182033) // #182033
    val subtleInk = Color(0xFF66728A) // #66728a
    val background = Color(0xFFF4F7FB) // #f4f7fb
    val cardBackground = Color.White.copy(alpha = 0.92f)
    val positive = accent

    val ringGradient = Brush.linearGradient(
        colors = listOf(primary, accent),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    val cardCorner = 24.dp
    val controlCorner = 14.dp
}
