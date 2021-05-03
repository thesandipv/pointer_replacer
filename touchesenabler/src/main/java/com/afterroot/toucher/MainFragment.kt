/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.toucher

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MainFragment : PreferenceFragmentCompat() {

    private lateinit var config: FirebaseRemoteConfig
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var interstitialAd: InterstitialAd
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var showTouchPref: SwitchPreferenceCompat
    private val isDisableAds: Boolean get() = sharedPreferences.getBoolean(getString(R.string.key_disable_ads), false)
    private var dialog: AlertDialog? = null

    private val currentSetting
        get() = Settings.System.getInt(requireActivity().contentResolver, getString(R.string.key_show_touches))


    @SuppressLint("CommitPrefEdits")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        editor = sharedPreferences.edit()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this.requireActivity())

        prefShowTouches()
        prefOthers()

        val isFirstInstall = sharedPreferences.getBoolean("first_install_2", true)
        if (isFirstInstall) {
            Bundle().apply {
                putString("Device_Name", Build.DEVICE)
                putString("Device_Model", Build.MODEL)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
                putString("AppVersion", BuildConfig.VERSION_CODE.toString())
                putString("Package", BuildConfig.APPLICATION_ID)
                firebaseAnalytics.logEvent("DeviceInfo2", this)
            }
            editor.putBoolean("first_install_2", false).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = FirebaseRemoteConfig.getInstance()
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build().apply {
                config.setConfigSettingsAsync(this)
            }

        config.fetch(config.info.configSettings.minimumFetchIntervalInSeconds)
            .addOnCompleteListener(this.requireActivity()) { task ->
                if (task.isSuccessful) {
                    config.activate()
                    prefVersion()
                    setUpAds()
                } else {
                    Log.d(TAG, "onCreate: Error getting RemoteConfig")
                }
            }
    }

    //Version Preference
    private fun prefVersion() {
        preferenceScreen.findPreference<Preference>(getString(R.string.key_version))?.apply {
            title = String.format(
                getString(R.string.format_version),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
            if (config.getLong(REMOTE_CONFIG_LATEST_BUILD) > BuildConfig.VERSION_CODE) {
                summary = getString(R.string.msg_update_available)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse(getString(R.string.url_play_store_app_page))
                    startActivity(intent)
                    true
                }
            }
        }
    }

    //Main Show touches Preference
    private fun prefShowTouches() {
        showTouchPref = preferenceScreen.findPreference(getString(R.string.key_show_touches))!!
        showTouchPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val i = Intent().apply {
                action = ACTION_OPEN_TEL
                putExtra("com.afterroot.toucher.EXTRA_TOUCH_VAL", if (newValue == true) 1 else 0)
            }
            if (i.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(i, RC_OPEN_TEL)
            }
            true
        }
    }

    //Other Preferences
    private fun prefOthers() {
        //Open Other Apps on Play Store
        preferenceScreen.findPreference<Preference>(getString(R.string.key_other_apps))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_NAME, getString(R.string.key_other_apps))
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_developer))
                    startActivity(this)
                }
                true
            }

        //Rate On Google Play
        /*preferenceScreen.findPreference<Preference>(getString(R.string.key_rate_on_g_play))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.ITEM_NAME,
                        getString(R.string.key_rate_on_g_play)
                    )
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_app_page))
                    startActivity(this)
                }
                true
            }*/
    }

    override fun onResume() {
        super.onResume()
        try {
            val showTouchesCurr = currentSetting == 1
            editor.putBoolean(getString(R.string.key_show_touches), showTouchesCurr).apply()
            showTouchPref.isChecked = showTouchesCurr
        } catch (e: Settings.SettingNotFoundException) {
            requireActivity().root_layout.snackbar(getString(R.string.msg_error))
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
                        requireActivity().root_layout.snackbar(getString(R.string.msg_done))
                        if (interstitialAd.isLoaded && !isDisableAds) {
                            interstitialAd.show()
                        }
                    }
                    2 -> { //Write Setting Permission not Granted
                        requireActivity().root_layout.snackbar(getString(R.string.msg_secure_settings_permission))
                            .setAction(getString(R.string.action_grant)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data = Uri.parse("package:com.afterroot.toucher")
                                    startActivity(intent)
                                }

                            }
                    }
                    3 -> requireActivity().root_layout.snackbar(getString(R.string.msg_error)) //Other error
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpAds() {
        interstitialAd = InterstitialAd(this.requireActivity())
        interstitialAd.apply {
            adUnitId = getString(R.string.interstitial_ad_2_id)
            loadAd(AdRequest.Builder().build())
        }

        val watchAds =
            preferenceScreen.findPreference<Preference>(getString(R.string.key_watch_ads))!!
        watchAds.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (interstitialAd.isLoaded) {
                    interstitialAd.show()
                } else {
                    requireActivity().root_layout.snackbar(getString(R.string.msg_ad_not_loaded))
                }
                true
            }
            summary = getString(R.string.msg_ad_loading)
            isEnabled = false
        }

        interstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                super.onAdClosed()
                watchAds.apply {
                    summary = getString(R.string.msg_ad_loading)
                    isEnabled = false
                }
                interstitialAd.loadAd(AdRequest.Builder().build())
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                watchAds.apply {
                    summary = getString(R.string.msg_ad_loaded)
                    isEnabled = true
                }
            }
        }
    }

    companion object {
        const val ACTION_OPEN_TEL = "com.afterroot.action.OPEN_TOUCHER"
        const val RC_OPEN_TEL = 245
        const val REMOTE_CONFIG_LATEST_BUILD = "touch_enabler_latest_build"
        private const val TAG = "MainFragment"
    }

}