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

package com.afterroot.allusive;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;

import com.afollestad.materialdialogs.MaterialDialog;


public class SettingsActivity extends AppCompatActivity {

    private Preference mChooseColorPicker;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();
    }

    private void showSingleChoice(){
        int selectedIndex = mSharedPreferences.getInt("selectedIndex", 1);
        new MaterialDialog.Builder(this)
                .title(R.string.choose_color_picker)
                .items(R.array.CCItems)
                .itemsCallbackSingleChoice(selectedIndex, (dialog, view, which, text) -> {
                    mEditor.putInt("selectedIndex", which).apply();
                    if (which == 0) {
                        mEditor.putBoolean(getString(R.string.key_useMDCC), false).apply();
                    } else if (which == 1) {
                        mEditor.putBoolean(getString(R.string.key_useMDCC), true).apply();
                    }
                    updateCCSummary();
                    return true;
                })
                .positiveText(R.string.changelog_ok_button)
                .show();
    }

    private void updateCCSummary(){
        if (mSharedPreferences.getBoolean(getString(R.string.key_useMDCC), true)){
            mChooseColorPicker.setSummary("Material Color Picker");
        } else {
            mChooseColorPicker.setSummary("HSV Color Picker");
        }
    }

    @SuppressLint("ValidFragment")
    private class SettingsFragment extends PreferenceFragment {
        Preference maxPointerSize, maxPaddingSize;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

            mChooseColorPicker = findPreference(getString(R.string.key_useMDCC));
            updateCCSummary();
            mChooseColorPicker.setOnPreferenceClickListener(preference -> {
                showSingleChoice();
                return false;
            });

            maxPointerSize = findPreference(getString(R.string.key_maxPointerSize));
            maxPointerSize.setSummary(mSharedPreferences.getString(getString(R.string.key_maxPointerSize), "100"));
            maxPointerSize.setOnPreferenceClickListener(preference -> {
                new MaterialDialog.Builder(SettingsActivity.this)
                        .title(R.string.text_max_pointer_size)
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .inputRange(2, 3)
                        .input("Enter Max Pointer Size",
                                mSharedPreferences.getString(getString(R.string.key_maxPointerSize), "100"),
                                new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                mEditor.putString(getString(R.string.key_maxPointerSize), input.toString());
                                //mEditor.putInt(getString(R.string.key_maxPointerSize), Integer.parseInt(input.toString()));
                                mEditor.apply();
                                maxPointerSize.setSummary(input);
                            }
                        }).show();
                return false;
            });

            maxPaddingSize = findPreference(getString(R.string.key_maxPaddingSize));
            maxPaddingSize.setSummary(mSharedPreferences.getString(getString(R.string.key_maxPaddingSize), "25"));
            maxPaddingSize.setOnPreferenceClickListener(preference -> {
                new MaterialDialog.Builder(SettingsActivity.this)
                        .title(getString(R.string.key_maxPaddingSize))
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .inputRange(1, 3)
                        .input("Enter Max Padding Size",
                                mSharedPreferences.getString(getString(R.string.key_maxPaddingSize), "25"),
                                (dialog, input) -> {
                                    mEditor.putString(getString(R.string.key_maxPaddingSize), input.toString());
                                    mEditor.apply();
                                    maxPointerSize.setSummary(input);
                                }).show();
                return false;
            });
        }
    }
}
