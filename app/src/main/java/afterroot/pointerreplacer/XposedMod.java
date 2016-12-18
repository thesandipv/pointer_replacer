/*
 * Copyright (C) 2016 Sandip Vaghela
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

package afterroot.pointerreplacer;

import android.content.res.XResources;
import android.graphics.drawable.Drawable;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private Drawable drawable;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences XSharedPreferences = new XSharedPreferences("afterroot.pointerreplacer");
        XSharedPreferences.makeWorldReadable();
        String POINTER_PATH = XSharedPreferences.getString("POINTER_PATH", "/data/data/afterroot.pointerreplacer/files/pointer.png");
        drawable = Drawable.createFromPath(POINTER_PATH);
        XposedBridge.log("Loaded Pointer from " + POINTER_PATH);
        XposedBridge.log(new Throwable());
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        XResources.setSystemWideReplacement("android", "drawable", "pointer_spot_touch", new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                XposedBridge.log("Created Pointer Drawable");
                XposedBridge.log(new Throwable());
                return drawable;
            }
        });
    }
}