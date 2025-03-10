/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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
package com.afterroot.allusive2.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlin.math.max
import kotlin.math.min

class BottomNavigationBehavior<V : View>(context: Context, attrs: AttributeSet) :
  CoordinatorLayout.Behavior<V>(context, attrs) {

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int,
  ): Boolean = axes == ViewCompat.SCROLL_AXIS_VERTICAL

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int,
  ) {
    super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    child.translationY = max(0f, min(child.height.toFloat(), child.translationY + dy))
  }

  override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
    if (dependency is Snackbar.SnackbarLayout) {
      updateSnackbar(child, dependency)
    }
    return super.layoutDependsOn(parent, child, dependency)
  }

  private fun updateSnackbar(child: View, snackbarLayout: Snackbar.SnackbarLayout) {
    if (snackbarLayout.layoutParams is CoordinatorLayout.LayoutParams) {
      val params = snackbarLayout.layoutParams as CoordinatorLayout.LayoutParams

      params.anchorId = child.id
      params.anchorGravity = Gravity.TOP
      params.gravity = Gravity.TOP
      snackbarLayout.layoutParams = params
    }
  }
}

class BottomNavigationFABBehavior(context: Context?, attrs: AttributeSet?) :
  CoordinatorLayout.Behavior<View>(context, attrs) {

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int,
  ): Boolean = axes == ViewCompat.SCROLL_AXIS_VERTICAL ||
    super.onStartNestedScroll(
      coordinatorLayout,
      child,
      directTargetChild,
      target,
      axes,
      type,
    )

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray,
  ) {
    super.onNestedScroll(
      coordinatorLayout,
      child,
      target,
      dxConsumed,
      dyConsumed,
      dxUnconsumed,
      dyUnconsumed,
      type,
      consumed,
    )
    val fab = child as ExtendedFloatingActionButton

    if (dyConsumed > 0) {
      fab.shrink()
    } else if (dyConsumed < 0) {
      fab.extend()
    }
  }

  override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean =
    dependency is Snackbar.SnackbarLayout

  override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
    child.translationY = 0f
  }

  override fun onDependentViewChanged(
    parent: CoordinatorLayout,
    child: View,
    dependency: View,
  ): Boolean = updateButton(child, dependency)

  private fun updateButton(child: View, dependency: View): Boolean {
    if (dependency is Snackbar.SnackbarLayout) {
      val oldTranslation = child.translationY
      val height = dependency.height.toFloat()
      val newTranslation = dependency.translationY - height
      child.translationY = newTranslation

      return oldTranslation != newTranslation
    }
    return false
  }
}
