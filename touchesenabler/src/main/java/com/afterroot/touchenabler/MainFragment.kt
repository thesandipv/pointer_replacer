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
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.design.snackbar

class MainFragment : PreferenceFragmentCompat(), BillingProcessor.IBillingHandler {

    private lateinit var billingProcessor: BillingProcessor
    private lateinit var donatePreference: Preference
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var grantPermissionPref: Preference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var showTouchPref: SwitchPreferenceCompat
    private val _tag: String = "TouchEnabler"
    private var isReadyToPurchase: Boolean = false
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var interstitialAd: InterstitialAd

    @SuppressLint("CommitPrefEdits")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        editor = sharedPreferences.edit()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this.activity!!)

        showTouchPref = preferenceScreen.findPreference(getString(R.string.key_show_touches)) as SwitchPreferenceCompat
        showTouchPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            when (newValue) {
                true -> {
                    setShowTouches(1)
                    activity!!.root_layout.snackbar(getString(R.string.msg_touch_enabled))
                }
                else -> {
                    setShowTouches(0)
                    activity!!.root_layout.snackbar(getString(R.string.msg_touch_disabled))
                }
            }
            true
        }

        //Grant Write Settings Permission if not
        grantPermissionPref = preferenceScreen.findPreference(getString(R.string.key_grant_permission))
        if (isMUp()) {
            grantPermissionPref.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + activity!!.packageName)
                startActivity(intent)
                true
            }
        }

        //Open Other Apps on Play Store
        preferenceScreen.findPreference(getString(R.string.key_other_apps)).setOnPreferenceClickListener {
            val bundle = Bundle()
            with(bundle) {
                putString(FirebaseAnalytics.Param.ITEM_NAME, getString(R.string.key_other_apps))
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_play_store_developer))
            startActivity(intent)
            true
        }

        //Rate On Google Play
        preferenceScreen.findPreference(getString(R.string.key_rate_on_g_play)).setOnPreferenceClickListener {
            val bundle = Bundle()
            with(bundle) {
                putString(FirebaseAnalytics.Param.ITEM_NAME, getString(R.string.key_rate_on_g_play))
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_play_store_app_page))
            startActivity(intent)
            true
        }

        setUpAds()

        val isFirstInstall = sharedPreferences.getBoolean("first_install", true)
        if (isFirstInstall) {
            val bundle = Bundle()
            with(bundle) {
                putString("Device_Name", Build.DEVICE)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
            }
            firebaseAnalytics.logEvent("DeviceInfo", bundle)
            editor.putBoolean("first_install", false)
            Log.d(_tag, "DeviceInfo: $bundle")
        }
    }

    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        firebaseRemoteConfig.setConfigSettings(configSettings)

        val isUsingDeveloperMode = firebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled
        val cacheExpiration: Long = if (isUsingDeveloperMode) {
            0
        } else {
            3600
        }

        firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener(this.activity!!) { task ->
            if (task.isSuccessful) {
                firebaseRemoteConfig.activateFetched()

                if (!BillingProcessor.isIabServiceAvailable(this.activity!!)) {
                    activity!!.root_layout.snackbar(getString(R.string.msg_info_iab_na))
                }

                billingProcessor = BillingProcessor.newBillingProcessor(
                        this.activity!!,
                        firebaseRemoteConfig.getString(INAPP_LICENCE_KEY),
                        this
                )
                billingProcessor.initialize()

                val purchased = billingProcessor.isPurchased(firebaseRemoteConfig.getString(PRODUCT_ID_KEY_1))
                //Donate Preference
                donatePreference.apply {
                    isEnabled = !purchased
                    summary = if (purchased) getString(R.string.msg_donation_done) else getString(R.string.pref_summary_donation)
                }

                //Version Preference
                preferenceScreen.findPreference(getString(R.string.key_version)).apply {
                    title = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    if (firebaseRemoteConfig.getLong("touch_enabler_latest_build") > BuildConfig.VERSION_CODE) {
                        summary = getString(R.string.msg_update_available)
                        setOnPreferenceClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(getString(R.string.url_play_store_app_page))
                            startActivity(intent)
                            true
                        }
                    }
                }
            } else {
                Log.d(_tag, "onCreate: Error getting RemoteConfig")
            }

        }

        donatePreference = findPreference(getString(R.string.key_pref_donate))
        donatePreference.setOnPreferenceClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!, firebaseRemoteConfig.getString(PRODUCT_ID_KEY_1), Toast.LENGTH_SHORT).show()
            }
            billingProcessor.purchase(this.activity, firebaseRemoteConfig.getString(PRODUCT_ID_KEY_1))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!checkSystemWritePermission() && isMUp()) {
            showTouchPref.isEnabled = false
            preferenceScreen.addPreference(grantPermissionPref)
        } else {
            preferenceScreen.removePreference(grantPermissionPref)
            showTouchPref.isEnabled = true
        }

        val showTouchesCurr = Settings.System.getInt(activity!!.contentResolver, getString(R.string.key_show_touches)) == 1
        editor.putBoolean(getString(R.string.key_show_touches), showTouchesCurr).apply()
        showTouchPref.isChecked = showTouchesCurr
    }

    override fun onDestroy() {
        super.onDestroy()
        billingProcessor.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBillingInitialized() {
        if (BuildConfig.DEBUG) {
            activity!!.root_layout.snackbar("Billing Initialized")
        }
        isReadyToPurchase = true
    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        activity!!.root_layout.snackbar(getString(R.string.msg_donation_thanks))
        donatePreference.apply {
            isEnabled = false
            summary = getString(R.string.msg_donation_done)
        }
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        activity!!.root_layout.snackbar("${getString(R.string.msg_billing_error)} $errorCode")
        donatePreference.summary = "${getString(R.string.msg_billing_error)} $errorCode"
    }

    private fun setUpAds() {
        interstitialAd = InterstitialAd(this.activity!!)
        interstitialAd.apply {
            adUnitId = if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/1033173712"
            } else {
                getString(R.string.interstitial_ad_2_id)
            }
            loadAd(AdRequest.Builder().build())
        }

        val watchAds = preferenceScreen.findPreference(getString(R.string.key_watch_ads))
        watchAds.apply {
            setOnPreferenceClickListener {
                if (interstitialAd.isLoaded) {
                    interstitialAd.show()
                } else {
                    activity!!.root_layout.snackbar("Ad Not Loaded yet")
                }
                true
            }
            summary = "Ad Loading"
            isEnabled = false
        }

        interstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                super.onAdClosed()
                watchAds.apply {
                    summary = "Ad Loading"
                    isEnabled = false
                }
                interstitialAd.loadAd(AdRequest.Builder().build())
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                watchAds.apply {
                    summary = "Ad Loaded"
                    isEnabled = true
                }
            }

        }
    }

    private fun checkSystemWritePermission(): Boolean {
        val retVal: Boolean
        if (isMUp()) {
            retVal = Settings.System.canWrite(activity)
            Log.d(_tag, "Can Write Settings: $retVal")
            if (!retVal) {
                activity!!.root_layout.snackbar(getString(R.string.msg_setting_not_allowed))
            }
        } else retVal = true
        return retVal
    }

    private fun setShowTouches(touches: Int) {
        if (checkSystemWritePermission()) {
            try {
                Settings.System.putInt(activity!!.contentResolver, getString(R.string.key_show_touches), touches)
            } catch (e: Exception) {
                Log.e(_tag, e.toString())
                Crashlytics.logException(e)
                Toast.makeText(activity, getString(R.string.msg_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, getString(R.string.msg_secure_settings_permission), Toast.LENGTH_SHORT).show()
        }

    }

    private fun isMUp(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    companion object {
        const val INAPP_LICENCE_KEY = "inapp_licence_key"
        const val PRODUCT_ID_KEY_1 = "product_id_1"
        const val PRODUCT_ID_KEY_2 = "product_id_2"
    }
}