/*
 * Copyright (C) 2016-2017 Sandip Vaghela
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

package com.afterroot.allusive

import android.content.res.XResources
import android.graphics.drawable.Drawable

import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_InitPackageResources

class XposedMod : IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private var drawable: Drawable? = null

    @Throws(Throwable::class)
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val XSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID)
        XSharedPreferences.makeWorldReadable()
        val POINTER_PATH = XSharedPreferences.getString("POINTER_PATH", "/data/data/" + BuildConfig.APPLICATION_ID + "/files/pointer.png")
        drawable = Drawable.createFromPath(POINTER_PATH)
        XposedBridge.log("Loaded Pointer from " + POINTER_PATH!!)
        XposedBridge.log(Throwable())
    }

    @Throws(Throwable::class)
    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        XResources.setSystemWideReplacement("android", "drawable", "pointer_spot_touch", object : XResources.DrawableLoader() {
            @Throws(Throwable::class)
            override fun newDrawable(xResources: XResources, i: Int): Drawable? {
                XposedBridge.log("Created Pointer Drawable")
                XposedBridge.log(Throwable())
                return drawable
            }
        })
    }
}