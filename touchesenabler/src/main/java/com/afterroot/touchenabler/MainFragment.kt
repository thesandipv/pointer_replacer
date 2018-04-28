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

package com.afterroot.touchenabler

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.provider.Settings

class MainFragment : PreferenceFragment() {

    lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_main)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        sharedPreferences.registerOnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == getString(R.string.key_show_touches)) {
                val showTouches = sharedPrefs.getBoolean(getString(R.string.key_show_touches), false)

                if (showTouches) {
                    setShowTouches(1)
                } else {
                    setShowTouches(0)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val showTouches = sharedPreferences.getBoolean(getString(R.string.key_show_touches), false)

        if (showTouches) {
            setShowTouches(1)
        } else {
            setShowTouches(0)
        }
    }

    private fun setShowTouches(touches: Int) {
        Settings.System.putInt(activity.contentResolver,
                getString(R.string.key_show_touches), touches)
    }
}