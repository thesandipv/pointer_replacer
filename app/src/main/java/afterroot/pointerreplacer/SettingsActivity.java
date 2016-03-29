/*
 * Copyright (C) 2016 Sandip Vaghela (AfterROOT)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package afterroot.pointerreplacer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

import de.psdev.licensesdialog.LicensesDialog;
import yuku.ambilwarna.AmbilWarnaDialog;


public class SettingsActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback {

    Preference mPointerCardPref, mChooseColorPicker;
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;
    Boolean isUseMDCC;
    int oldColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Utils.setNightModeEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();
    }

    public int getOldColor(){
        oldColor = mSharedPreferences.getInt(getString(R.string.key_oldCardColor), -1);
        return oldColor;
    }

    public void buildCC(){
        getOldColor();
        new ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(true)
                .dynamicButtonColor(false)
                .preselect(oldColor)
                .show();
    }

    /**Show color picker dialog.**/
    public void showColorPicker() {
        getOldColor();
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, oldColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                //do nothing.
            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mEditor.putInt(getString(R.string.key_cardColor), color).apply();
                mEditor.putInt(getString(R.string.key_oldCardColor), color).apply();
            }
        });
        dialog.show();
    }

    public void showSingleChoice(){
        int selectedIndex = mSharedPreferences.getInt("selectedIndex", 1);
        new MaterialDialog.Builder(this)
                .title(R.string.choose_color_picker)
                .items(R.array.CCItems)
                .itemsCallbackSingleChoice(selectedIndex, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        /**
                         * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                         * returning false here won't allow the newly selected radio button to actually be selected.
                         **/
                        Toast.makeText(getBaseContext(), text + " selected", Toast.LENGTH_SHORT).show();
                        mEditor.putInt("selectedIndex", which).apply();
                        if (which == 0) {
                            mEditor.putBoolean(getString(R.string.key_useMDCC), false).apply();
                        } else if (which == 1) {
                            mEditor.putBoolean(getString(R.string.key_useMDCC), true).apply();
                        }
                        return true;
                    }
                })
                .positiveText(R.string.changelog_ok_button)
                .show();
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
        String keyOldColor = getString(R.string.key_oldCardColor);
        mEditor.putInt(getString(R.string.key_cardColor), selectedColor).apply();
        mEditor.putInt(keyOldColor, selectedColor).apply();
    }

    @SuppressLint("ValidFragment")
    public class SettingsFragment extends PreferenceFragment {
        Preference licenses, maxPointerSize, prefUpdate;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Utils.setNightModeEnabled(false);
            addPreferencesFromResource(R.xml.pref_settings);

            licenses = findPreference("licenses");
            licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new LicensesDialog.Builder(SettingsActivity.this)
                            .setNotices(R.raw.notices)
                            .build()
                            .showAppCompat();
                    return false;
                }
            });

            mPointerCardPref = findPreference(getString(R.string.key_cardColor));
            mPointerCardPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    isUseMDCC = mSharedPreferences.getBoolean(getString(R.string.key_useMDCC), true);
                    if (isUseMDCC){
                        buildCC();
                    } else {
                        showColorPicker();
                    }
                    return false;
                }
            });

            mChooseColorPicker = findPreference(getString(R.string.key_useMDCC));
            mChooseColorPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSingleChoice();
                    return false;
                }
            });

            maxPointerSize = findPreference(getString(R.string.key_maxPointerSize));
            maxPointerSize.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
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
                                }
                            }).show();
                    return false;
                }
            });

            prefUpdate = findPreference("pref_update");
            prefUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(SettingsActivity.this, UpdateActivity.class));
                    return false;
                }
            });
        }
    }
}
