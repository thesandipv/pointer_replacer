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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.Helper
import com.afterroot.allusive.R

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private var mChooseColorPicker: Preference? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private lateinit var maxPointerSize: Preference
    private lateinit var maxPaddingSize: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings)
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