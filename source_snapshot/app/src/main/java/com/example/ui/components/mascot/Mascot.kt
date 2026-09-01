package com.example.ui.components.mascot

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.PlannerMotion

enum class MascotState(@DrawableRes val drawable: Int) {
    Idle(R.drawable.mascot_idle),
    Loading(R.drawable.mascot_loading),
    Success(R.drawable.mascot_success),
    Error(R.drawable.mascot_error),
    Empty(R.drawable.mascot_empty),
    Hint(R.drawable.mascot_hint),
    FoldersEmpty(R.drawable.mascot_folders_empty),
    FavoritesEmpty(R.drawable.mascot_favorites_empty),
    PlansEmpty(R.drawable.mascot_plans_empty),
    Guide(R.drawable.mascot_guide)
}

@Composable
fun PlannerMascot(
    state: MascotState,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    loadingTravel: Dp = 3.dp
) {
    val offset = if (state == MascotState.Loading) {
        val transition = rememberInfiniteTransition(label = "mascot_loading")
        transition.animateFloat(
            initialValue = -loadingTravel.value,
            targetValue = loadingTravel.value,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mascot_loading_offset"
        ).value.dp
    } else {
        0.dp
    }
    Image(
        painter = painterResource(state.drawable),
        contentDescription = contentDescription,
        modifier = modifier.offset(y = offset),
        contentScale = ContentScale.Fit
    )
}
