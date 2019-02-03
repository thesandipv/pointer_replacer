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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.R
import com.afterroot.allusive.utils.Helper
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.snackbar

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private var mChooseColorPicker: Preference? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private lateinit var maxPointerSize: Preference
    private lateinit var maxPaddingSize: Preference
    private lateinit var showTouches: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)

    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSharedPreferences = Helper.getSharedPreferences(activity!!)
        mEditor = mSharedPreferences!!.edit()

        mChooseColorPicker = findPreference(getString(R.string.key_useMDCC))
        updateCCSummary()
        mChooseColorPicker!!.setOnPreferenceClickListener {
            showSingleChoice()
            false
        }

        maxPointerSize = findPreference(getString(R.string.key_maxPointerSize))
        maxPointerSize.summary = mSharedPreferences!!.getString(getString(R.string.key_maxPointerSize), "100")
        maxPointerSize.setOnPreferenceClickListener {
            MaterialDialog.Builder(activity!!)
                    .title(R.string.text_max_pointer_size)
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .inputRange(2, 3)
                    .input("Enter Max Pointer Size",
                            mSharedPreferences!!.getString(getString(R.string.key_maxPointerSize), "100")
                    ) { _, input ->
                        mEditor!!.putString(getString(R.string.key_maxPointerSize), input.toString())
                        mEditor!!.apply()
                        maxPointerSize.summary = input
                    }.show()
            false
        }

        maxPaddingSize = findPreference(getString(R.string.key_maxPaddingSize))
        maxPaddingSize.summary = mSharedPreferences!!.getString(getString(R.string.key_maxPaddingSize), "25")
        maxPaddingSize.setOnPreferenceClickListener {
            MaterialDialog.Builder(activity!!)
                    .title(getString(R.string.key_maxPaddingSize))
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .inputRange(1, 3)
                    .input("Enter Max Padding Size",
                            mSharedPreferences!!.getString(getString(R.string.key_maxPaddingSize), "25")
                    ) { _, input ->
                        mEditor!!.putString(getString(R.string.key_maxPaddingSize), input.toString())
                        mEditor!!.apply()
                        maxPointerSize.summary = input
                    }.show()
            false
        }

        showTouches = findPreference("show_touches") as SwitchPreferenceCompat
        showTouches.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue == true) {
                setShowTouches(1)
                activity!!.container.snackbar("Touches Enabled")
            } else {
                setShowTouches(0)
                activity!!.container.snackbar("Touches Disabled")
            }
            return@OnPreferenceChangeListener true
        }

        findPreference("pref_version").apply {
            summary = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
    }

    override fun onResume() {
        super.onResume()

        val showTouchesCurr = Settings.System.getInt(activity!!.contentResolver, "show_touches") == 1
        mEditor!!.putBoolean("show_touches", showTouchesCurr).apply()
        showTouches.isChecked = showTouchesCurr
    }

    private fun isMUp(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun checkSystemWritePermission(): Boolean {
        var retVal = false
        if (isMUp()) {
            retVal = Settings.System.canWrite(activity)
            Log.d(TAG, "Can Write Settings: $retVal")
            if (retVal) {
                Toast.makeText(activity, "Write allowed", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "Write not allowed", Toast.LENGTH_LONG).show()
            }
        } else retVal = true
        return retVal
    }

    val TAG = "SettingsFragment"
    private fun setShowTouches(touches: Int) {
        if (checkSystemWritePermission()) {
            try {
                Settings.System.putInt(activity!!.contentResolver, "show_touches", touches)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                Crashlytics.logException(e)
                Toast.makeText(activity, "Opps! Some Error occurred.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, "Please grant app to write Secure Settings permission", Toast.LENGTH_SHORT).show()
        }

    }

    private fun showSingleChoice() {
        val selectedIndex = mSharedPreferences!!.getInt("selectedIndex", 1)
        MaterialDialog.Builder(activity!!)
                .title(R.string.choose_color_picker)
                .items(R.array.CCItems)
                .itemsCallbackSingleChoice(selectedIndex) { _, _, which, _ ->
                    mEditor!!.putInt("selectedIndex", which).apply()
                    if (which == 0) {
                        mEditor!!.putBoolean(getString(R.string.key_useMDCC), false).apply()
                    } else if (which == 1) {
                        mEditor!!.putBoolean(getString(R.string.key_useMDCC), true).apply()
                    }
                    updateCCSummary()
                    true
                }
                .positiveText(R.string.changelog_ok_button)
                .show()
    }

    private fun updateCCSummary() {
        if (mSharedPreferences!!.getBoolean(getString(R.string.key_useMDCC), true)) {
            mChooseColorPicker!!.summary = "Material Color Picker"
        } else {
            mChooseColorPicker!!.summary = "HSV Color Picker"
        }
    }
}