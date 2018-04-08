/*
 * Copyright (C) 2016-2018 Sandip Vaghela
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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment

class MainFragment : PreferenceFragment() {

    var prefOpenDash: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_main)

        prefOpenDash = findPreference(getString(R.string.pref_key_open_dash))

        if (isAppInstalled(activity, getString(R.string.dash_package_name))) {
            prefOpenDash!!.apply {
                title = "Open Pointers Dashboard"
                summary = "for downloading and applying pointer"
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val intent = activity!!.packageManager.getLaunchIntentForPackage(getString(R.string.dash_package_name))
                    if (intent != null) {
                        startActivity(intent)
                    }
                    return@OnPreferenceClickListener true
                }
            }
        } else {
            prefOpenDash!!.apply {

            }
        }
    }

    fun isAppInstalled(context: Context, pName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(pName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}