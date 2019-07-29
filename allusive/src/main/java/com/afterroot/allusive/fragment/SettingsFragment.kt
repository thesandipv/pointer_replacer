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
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.Constants.ACTION_OPEN_TEL
import com.afterroot.allusive.Constants.EXTRA_TOUCH_VAL
import com.afterroot.allusive.Constants.RC_OPEN_TEL
import com.afterroot.allusive.Constants.TEL_P_NAME
import com.afterroot.allusive.R
import com.afterroot.allusive.utils.getMinPointerSize
import com.afterroot.allusive.utils.getPrefs
import com.afterroot.allusive.utils.isAppInstalled
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import java.io.File

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val _tag = "SettingsFragment"
    private var dialog: AlertDialog? = null
    private var preferences: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = context!!.getPrefs()

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.let { config ->
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                .build().apply {
                    config.setConfigSettingsAsync(this)
                }

            config.fetch(config.info.fetchTimeMillis).addOnCompleteListener(activity!!) { result ->
                if (result.isSuccessful) {
                    firebaseRemoteConfig.activate()
                }
            }
        }


        findPreference<Preference>(getString(R.string.key_use_material_cc))!!.apply {
            updateCCSummary(this)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showSingleChoice()
                false
            }
        }


        findPreference<Preference>(getString(R.string.key_maxPointerSize))!!.apply {
            summary =
                preferences!!.getString(getString(R.string.key_maxPointerSize), context!!.getMinPointerSize().toString())
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(activity!!).show {
                    title(res = R.string.text_max_pointer_size)
                    input(
                        hintRes = R.string.text_max_pointer_size,
                        prefill = preferences!!.getString(
                            getString(R.string.key_maxPointerSize),
                            context.getMinPointerSize().toString()
                        ),
                        inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 3, allowEmpty = false
                    ) { _, input ->
                        preferences!!.edit(true) { putString(getString(R.string.key_maxPointerSize), input.toString()) }
                        this@apply.summary = input
                    }
                }
                false
            }
        }

        findPreference<Preference>(getString(R.string.key_maxPaddingSize))!!.apply {
            summary = preferences!!.getString(getString(R.string.key_maxPaddingSize), "0")
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(activity!!).show {
                    title(res = R.string.key_maxPaddingSize)
                    input(
                        hint = "Enter Max Padding Size",
                        prefill = preferences!!.getString(getString(R.string.key_maxPaddingSize), "0"),
                        allowEmpty = false, maxLength = 3,
                        inputType = InputType.TYPE_CLASS_NUMBER
                    ) { _, input ->
                        preferences!!.edit(true) { putString(getString(R.string.key_maxPaddingSize), input.toString()) }
                        this@apply.summary = input
                    }.show()
                }
                false
            }
        }

        findPreference<SwitchPreferenceCompat>(getString(R.string.key_show_touches))!!
            .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val i = Intent().apply {
                action = ACTION_OPEN_TEL
                putExtra(EXTRA_TOUCH_VAL, if (newValue == true) 1 else 0)
            }
            if (i.resolveActivity(activity!!.packageManager) != null) {
                startActivityForResult(i, RC_OPEN_TEL)
            } else {
                Toast.makeText(activity!!, "Please install Extension First", Toast.LENGTH_SHORT).show()
                installExtensionDialog().show()
            }
            true
        }

        findPreference<Preference>("pref_version")?.apply {
            summary = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }

        findPreference<Preference>("licenses")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                LicensesDialog.Builder(context!!).setNotices(R.raw.notices).build().show()
                return@OnPreferenceClickListener true
            }
        }
    }

    private fun installExtensionDialog(): AlertDialog {
        dialog = AlertDialog.Builder(activity!!).setTitle(getString(R.string.title_install_ext_dialog))
            .setMessage(getString(R.string.msg_install_ext_dialog))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.dialog_button_cancel)) { _, _ ->

            }
            .setPositiveButton(getString(R.string.dialog_button_install)) { _, _ ->
                val reference = FirebaseStorage.getInstance().reference.child("updates/tapslegacy-release.apk")
                val tmpFile = File(context!!.cacheDir, "app.apk")
                activity!!.container.longSnackbar(getString(R.string.msg_downloading_ext)).anchorView = activity!!.navigation
                reference.getFile(tmpFile).addOnSuccessListener {
                    activity!!.container.snackbar(getString(R.string.msg_ext_downloaded)).anchorView = activity!!.navigation
                    Log.d(_tag, "installExtensionDialog: ${Uri.fromFile(tmpFile)}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val uri = FileProvider.getUriForFile(
                            context!!.applicationContext,
                            BuildConfig.APPLICATION_ID + ".provider", tmpFile
                        )
                        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(installIntent)
                    } else {
                        val installIntent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(
                                Uri.fromFile(tmpFile),
                                "application/vnd.android.package-archive"
                            )
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(installIntent)
                    }

                }
            }.setNeutralButton("Learn More") { _, _ ->
                activity!!.browse("https://pointerreplacer.page.link/ext_learn_more")
            }.create()
        return dialog as AlertDialog
    }


    override fun onResume() {
        super.onResume()

        if (!activity!!.isAppInstalled(TEL_P_NAME)) {
            installExtensionDialog().show()
        }
        try {
            val showTouchesCurr =
                Settings.System.getInt(activity!!.contentResolver, getString(R.string.key_show_touches)) == 1
            preferences!!.edit(true) {
                putBoolean(getString(R.string.key_show_touches), showTouchesCurr)
            }
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_show_touches))!!.isChecked = showTouchesCurr
        } catch (e: Settings.SettingNotFoundException) {
            activity!!.container.snackbar(getString(R.string.msg_error)).anchorView = activity!!.navigation
        }
    }

    override fun onPause() {
        super.onPause()
        dialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (requestCode == RC_OPEN_TEL) {
                super.onActivityResult(requestCode, resultCode, data)
                when (resultCode) {
                    1 -> { //Result OK
                        activity!!.container.snackbar("Done").anchorView = activity!!.navigation
                        /* if (interstitialAd.isLoaded) {
                             interstitialAd.show()
                         }*/
                    }
                    2 -> { //Write Setting Permission not Granted
                        activity!!.container.snackbar(getString(R.string.msg_secure_settings_permission))
                            .setAction("GRANT") {
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data = Uri.parse("package:$TEL_P_NAME")
                                    startActivity(intent)
                                }

                            }.anchorView = activity!!.navigation
                    }
                    3 -> activity!!.container.snackbar(getString(R.string.msg_error)).anchorView =
                        activity!!.navigation //Other error
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSingleChoice() {
        val selectedIndex = preferences!!.getInt("selectedIndex", 1)
        MaterialDialog(activity!!).show {
            title(R.string.choose_color_picker)
            listItemsSingleChoice(res = R.array.CCItems, initialSelection = selectedIndex) { _, index, _ ->
                preferences!!.edit(true) {
                    putInt("selectedIndex", index)
                    putBoolean(getString(R.string.key_use_material_cc), index != 0)
                }
                updateCCSummary(findPreference(getString(R.string.key_use_material_cc))!!)
            }
            positiveButton(R.string.changelog_ok_button)
        }
    }

    private fun updateCCSummary(preference: Preference) {
        preference.summary =
            if (preferences!!.getBoolean(getString(R.string.key_use_material_cc), true)) {
                "Material Color Picker"
            } else {
                "HSV Color Picker"
            }
    }
}