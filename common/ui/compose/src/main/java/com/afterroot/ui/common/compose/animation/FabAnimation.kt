/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.ui.common.compose.animation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

enum class FabState {
  Visible,
  Invisible,
}

@Composable
fun AnimatedFab(
  fab: @Composable (
    animationState: MutableState<FabState>,
    scale: State<Float>,
    alpha: State<Float>,
  ) -> Unit,
) {
  val animationTargetState = remember { mutableStateOf(FabState.Invisible) }
  val transition = updateTransition(targetState = animationTargetState.value, label = "")

  val alpha = transition.animateFloat(
    transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
    label = "",
  ) {
    if (it == FabState.Invisible) 0f else 1f
  }

  val scale = transition.animateFloat(transitionSpec = {
    tween(durationMillis = ANIMATION_DURATION)
  }, label = "") {
    if (it == FabState.Invisible) 0.5f else 1f
  }

  fab(animationTargetState, scale, alpha)
}

private const val ANIMATION_DURATION = 400
