/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import com.afterroot.allusive2.R
import com.google.android.material.textview.MaterialTextView

class LoginCustomView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

  private val customBack: AppCompatImageView
  private val unsplashAttribution: MaterialTextView

  init {
    inflate(context, R.layout.login_custom_view, this).apply {
      customBack = findViewById(R.id.login_custom_background)
      unsplashAttribution = findViewById(R.id.unsplash_attribution_text)
      loadBackgroundImage()
    }
  }

  private fun loadBackgroundImage() {
    // TODO
  }
}
