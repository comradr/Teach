package com.example.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object PlannerMotion {
    const val Press = 110
    const val Fade = 180
    const val Standard = 240
    const val Expand = 280
    const val Screen = 300

    fun <T> fade(): FiniteAnimationSpec<T> = tween(Fade)
    fun <T> standard(): FiniteAnimationSpec<T> = tween(Standard)
    fun <T> expand(): FiniteAnimationSpec<T> = tween(Expand)
    fun <T> softSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
