/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference
import java.io.InputStream

/**
 * Created by Sandip on 14-12-2017.
 */
@GlideModule
class MyAppGlide : AppGlideModule() {
  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    registry.append(
      StorageReference::class.java,
      InputStream::class.java,
      FirebaseImageLoader.Factory(),
    )
  }
}
