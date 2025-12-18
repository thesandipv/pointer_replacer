/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

import android.annotation.SuppressLint
import android.content.res.XResources
import android.graphics.drawable.Drawable
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_InitPackageResources

class XposedMod :
  IXposedHookZygoteInit,
  IXposedHookInitPackageResources {

  private var drawable: Drawable? = null
  private var mousePointer: Drawable? = null

  @SuppressLint("SdCardPath")
  @Throws(Throwable::class)
  override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
    val xSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID)
    xSharedPreferences.makeWorldReadable()
    val dataPath = "/data/data/${BuildConfig.APPLICATION_ID}/files/"
    val pointerPath = xSharedPreferences.getString("POINTER_PATH", dataPath + "pointer.png")
    val mousePath = xSharedPreferences.getString("MOUSE_PATH", dataPath + "mouse.png")
    drawable = Drawable.createFromPath(pointerPath)
    mousePointer = Drawable.createFromPath(mousePath)
    XposedBridge.log("Loaded Pointer from " + pointerPath!!)
    XposedBridge.log("Loaded Mouse Pointer from " + mousePath!!)
    // XposedBridge.log(Throwable())
  }

  @Throws(Throwable::class)
  override fun handleInitPackageResources(
    resparam: XC_InitPackageResources.InitPackageResourcesParam,
  ) {
    XResources.setSystemWideReplacement(
      "android",
      "drawable",
      "pointer_spot_touch",
      object : XResources.DrawableLoader() {
        @Throws(Throwable::class)
        override fun newDrawable(xResources: XResources, i: Int): Drawable? {
          XposedBridge.log("Created Pointer Drawable")
          // XposedBridge.log(Throwable())
          return drawable
        }
      },
    )

    XResources.setSystemWideReplacement(
      "android",
      "drawable",
      "pointer_arrow",
      object : XResources.DrawableLoader() {
        @Throws(Throwable::class)
        override fun newDrawable(res: XResources?, id: Int): Drawable? {
          XposedBridge.log("Created Mouse Pointer Drawable")
          // XposedBridge.log(Throwable())
          return mousePointer
        }
      },
    )
  }
}
