/*
 * Copyright (C) 2020-2021 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    Visible, Invisible
}

@Composable
fun AnimatedFab(fab: @Composable (animationState: MutableState<FabState>, scale: State<Float>, alpha: State<Float>) -> Unit) {
    val animationTargetState = remember { mutableStateOf(FabState.Invisible) }
    val transition = updateTransition(targetState = animationTargetState.value, label = "")

    val alpha = transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) }, label = ""
    ) {
        if (it == FabState.Invisible) 0f else 1f
    }

    val scale = transition.animateFloat(transitionSpec = { tween(durationMillis = ANIMATION_DURATION) }, label = "") {
        if (it == FabState.Invisible) 0.5f else 1f
    }

    fab(animationTargetState, scale, alpha)
}

private const val ANIMATION_DURATION = 400
