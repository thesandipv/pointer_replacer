/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
