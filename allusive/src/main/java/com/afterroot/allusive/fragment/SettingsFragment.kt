/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.R
import com.afterroot.allusive.utils.getPrefs
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.snackbar

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var maxPaddingSize: Preference
    private lateinit var maxPointerSize: Preference
    private lateinit var mChooseColorPicker: Preference
    private lateinit var showTouches: SwitchPreferenceCompat
    private var preferences: SharedPreferences? = null
    val _tag = "SettingsFragment"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = context!!.getPrefs()

        mChooseColorPicker = findPreference(getString(R.string.key_useMDCC))!!
        updateCCSummary()
        mChooseColorPicker.setOnPreferenceClickListener {
            showSingleChoice()
            false
        }

        maxPointerSize = findPreference(getString(R.string.key_maxPointerSize))!!
        maxPointerSize.summary = preferences!!.getString(getString(R.string.key_maxPointerSize), "100")
        maxPointerSize.setOnPreferenceClickListener {
            MaterialDialog(activity!!).show {
                title(res = R.string.text_max_pointer_size)
                input(hint = "Enter Max Pointer Size",
                        prefill = preferences!!.getString(getString(R.string.key_maxPointerSize), "100"),
                        inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 3, allowEmpty = false) { _, input ->
                    preferences!!.edit(true) { putString(getString(R.string.key_maxPointerSize), input.toString()) }
                    maxPointerSize.summary = input
                }
            }
            false
        }

        maxPaddingSize = findPreference(getString(R.string.key_maxPaddingSize))!!
        maxPaddingSize.summary = preferences!!.getString(getString(R.string.key_maxPaddingSize), "25")
        maxPaddingSize.setOnPreferenceClickListener {
            MaterialDialog(activity!!).show {
                title(res = R.string.key_maxPaddingSize)
                input(hint = "Enter Max Padding Size",
                        prefill = preferences!!.getString(getString(R.string.key_maxPaddingSize), "25"),
                        allowEmpty = false, maxLength = 3,
                        inputType = InputType.TYPE_CLASS_NUMBER) { _, input ->
                    preferences!!.edit(true) { putString(getString(R.string.key_maxPaddingSize), input.toString()) }
                    maxPointerSize.summary = input
                }.show()
            }
            false
        }

        showTouches = findPreference("show_touches")!!
        showTouches.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                setShowTouches(1)
                activity!!.container.snackbar("Touches Enabled")
            } else {
                setShowTouches(0)
                activity!!.container.snackbar("Touches Disabled")
            }
            return@OnPreferenceChangeListener true
        }

        findPreference<Preference>("pref_version")?.apply {
            summary = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
    }

    override fun onResume() {
        super.onResume()

        val showTouchesCurr = Settings.System.getInt(activity!!.contentResolver, "show_touches") == 1
        preferences!!.edit(true) { putBoolean("show_touches", showTouchesCurr) }
        showTouches.isChecked = showTouchesCurr
    }

    private fun setShowTouches(touches: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(activity)) {
                try {
                    Settings.System.putInt(activity!!.contentResolver, "show_touches", touches)
                } catch (e: Exception) {
                    Log.e(_tag, e.toString())
                    Crashlytics.logException(e)
                    Toast.makeText(activity, "Opps! Some Error occurred.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Please grant app to write Secure Settings permission", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                Settings.System.putInt(activity!!.contentResolver, "show_touches", touches)
            } catch (e: Exception) {
                Log.e(_tag, e.toString())
                Crashlytics.logException(e)
                Toast.makeText(activity, "Opps! Some Error occurred.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showSingleChoice() {
        val selectedIndex = preferences!!.getInt("selectedIndex", 1)
        MaterialDialog(activity!!).show {
            title(R.string.choose_color_picker)
            listItemsSingleChoice(res = R.array.CCItems, initialSelection = selectedIndex) { _, index, _ ->
                preferences!!.edit(true) {
                    putInt("selectedIndex", index)
                    putBoolean(getString(R.string.key_useMDCC), index != 0)
                }
                updateCCSummary()
            }
            positiveButton(R.string.changelog_ok_button)
        }
    }

    private fun updateCCSummary() {
        if (preferences!!.getBoolean(getString(R.string.key_useMDCC), true)) {
            mChooseColorPicker.summary = "Material Color Picker"
        } else {
            mChooseColorPicker.summary = "HSV Color Picker"
        }
    }
}