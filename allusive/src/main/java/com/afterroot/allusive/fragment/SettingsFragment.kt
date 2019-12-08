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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.Constants.ACTION_OPEN_TEL
import com.afterroot.allusive.Constants.EXTRA_TOUCH_VAL
import com.afterroot.allusive.Constants.RC_OPEN_TEL
import com.afterroot.allusive.Constants.TEL_P_NAME
import com.afterroot.allusive.R
import com.afterroot.allusive.Settings
import com.afterroot.allusive.getMinPointerSize
import com.afterroot.allusive.model.SkuModel
import com.afterroot.core.extensions.isAppInstalled
import com.afterroot.core.extensions.showStaticProgressDialog
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.inject
import java.io.File

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var billingClient: BillingClient
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var interstitialAd: InterstitialAd
    private val settings: Settings by inject()
    private var dialog: AlertDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFirebaseConfig()
        setAppThemePref()
        setMaxPointerPaddingPref()
        setMaxPointerSizePref()
        setOpenSourceLicPref()
        setShowTouchPref()
        setVersionPref()
        setDonatePref(false)
        initBilling()
        setUpAds()
        setRateOnGPlay()
    }

    private fun initBilling() {
        billingClient =
            BillingClient.newBuilder(context!!).enablePendingPurchases().setListener { _, purchases ->
                val purchase = purchases?.first()
                if (purchase != null) { //Consume every time after successful purchase
                    val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    billingClient.consumeAsync(params) { result, purchaseToken ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchaseToken != null) {
                            Log.d(TAG, "initBilling: Purchase Done and Consumed")
                        } else Log.d(TAG, "initBilling: Purchase Done but not Consumed.")
                    }
                }
            }.build()
    }

    private fun initFirebaseConfig() {
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.let { config ->
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                .build().apply {
                    config.setConfigSettingsAsync(this)
                }

            config.fetch(config.info.configSettings.minimumFetchIntervalInSeconds)
                .addOnCompleteListener(activity!!) { result ->
                    if (result.isSuccessful) {
                        firebaseRemoteConfig.activate()
                        setDonatePref(true)
                    } else {
                        setDonatePref(false)
                    }
                }
        }
    }

    private fun setDonatePref(isEnable: Boolean) {
        findPreference<Preference>(getString(R.string.key_pref_donate))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                setUpBilling()
                return@OnPreferenceClickListener true
            }
            isEnabled = isEnable
        }
    }

    private fun setVersionPref() {
        findPreference<Preference>("pref_version")?.apply {
            summary =
                String.format(getString(R.string.str_format_version), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        }
    }

    private fun setOpenSourceLicPref() {
        findPreference<Preference>("licenses")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                OssLicensesMenuActivity.setActivityTitle("Licences").apply { }
                startActivity(Intent(context!!, OssLicensesMenuActivity::class.java))
                return@OnPreferenceClickListener true
            }
        }
    }

    private fun setAppThemePref() {
        findPreference<ListPreference>("key_app_theme")?.apply {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    getString(R.string.theme_device_default) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    getString(R.string.theme_light) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    getString(R.string.theme_dark) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    getString(R.string.theme_battery) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }

                return@OnPreferenceChangeListener true
            }
        }
    }

    private fun setShowTouchPref() {
        findPreference<SwitchPreferenceCompat>(getString(R.string.key_show_touches))!!
            .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val i = Intent().apply {
                action = ACTION_OPEN_TEL
                putExtra(EXTRA_TOUCH_VAL, if (newValue == true) 1 else 0)
            }
            if (i.resolveActivity(activity!!.packageManager) != null) {
                startActivityForResult(i, RC_OPEN_TEL)
            } else {
                Toast.makeText(activity!!, getString(R.string.msg_install_extension), Toast.LENGTH_SHORT).show()
                installExtensionDialog().show()
            }
            true
        }
    }

    private fun setMaxPointerPaddingPref() {
        findPreference<Preference>(getString(R.string.key_maxPaddingSize))!!.apply {
            summary = settings.maxPointerPadding.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(activity!!).show {
                    title(res = R.string.key_maxPaddingSize)
                    input(
                        hint = getString(R.string.input_hint_max_padding_size),
                        prefill = settings.maxPointerPadding.toString(),
                        allowEmpty = false, maxLength = 3,
                        inputType = InputType.TYPE_CLASS_NUMBER
                    ) { _, input ->
                        if (input.toString().toInt() > 0) {
                            settings.maxPointerPadding = input.toString().toInt()
                            this@apply.summary = input
                        } else {
                            activity!!.container.snackbar(String.format(getString(R.string.str_format_value_error), 0))
                                .anchorView = activity!!.navigation
                        }

                    }.show()
                }
                false
            }
        }
    }

    private fun setMaxPointerSizePref() {
        findPreference<Preference>(getString(R.string.key_maxPointerSize))!!.apply {
            summary = settings.maxPointerSize.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(activity!!).show {
                    title(res = R.string.text_max_pointer_size)
                    input(
                        hintRes = R.string.text_max_pointer_size,
                        prefill = settings.maxPointerSize.toString(),
                        inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 3, allowEmpty = false
                    ) { _, input ->
                        if (input.toString().toInt() > context.getMinPointerSize()) {
                            settings.maxPointerSize = input.toString().toInt()
                            this@apply.summary = input
                        } else {
                            activity!!.container.snackbar(
                                String.format(
                                    getString(R.string.str_format_value_error),
                                    context.getMinPointerSize()
                                )
                            ).anchorView = activity!!.navigation
                        }

                    }
                }
                false
            }
        }
    }

    private fun setUpAds() {
        interstitialAd = InterstitialAd(this.activity!!)
        interstitialAd.apply {
            adUnitId = if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/1033173712"
            } else {
                getString(R.string.ad_interstitial_1_id)
            }
            loadAd(AdRequest.Builder().build())
        }

        val watchAds =
            preferenceScreen.findPreference<Preference>(getString(R.string.key_watch_ads))!!
        watchAds.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (interstitialAd.isLoaded) {
                    interstitialAd.show()
                } else {
                    activity!!.container.snackbar(getString(R.string.msg_ad_not_loaded))
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

    private fun setRateOnGPlay() {
        preferenceScreen.findPreference<Preference>(getString(R.string.key_rate_on_g_play))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.ITEM_NAME,
                        getString(R.string.key_rate_on_g_play)
                    )
                    FirebaseAnalytics.getInstance(context!!).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_app_page))
                    startActivity(this)
                }
                true
            }

    }

    private fun installExtensionDialog(): AlertDialog {
        dialog = AlertDialog.Builder(activity!!).setTitle(getString(R.string.title_install_ext_dialog))
            .setMessage(getString(R.string.msg_install_ext_dialog))
            .setCancelable(false)
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->
                settings.isExtDialogCancelled = true
            }
            .setPositiveButton(getString(R.string.dialog_button_install)) { _, _ ->
                val reference = FirebaseStorage.getInstance().reference.child("updates/tapslegacy-release.apk")
                val tmpFile = File(context!!.cacheDir, "app.apk")
                activity!!.container.longSnackbar(getString(R.string.msg_downloading_ext)).anchorView = activity!!.navigation
                reference.getFile(tmpFile).addOnSuccessListener {
                    activity!!.container.snackbar(getString(R.string.msg_ext_downloaded)).anchorView = activity!!.navigation
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
            }.setNeutralButton(getString(R.string.button_text_learn_more)) { _, _ ->
                activity!!.browse(getString(R.string.url_learn_more))
            }.create()
        return dialog as AlertDialog
    }

    override fun onResume() {
        super.onResume()

        if (!activity!!.isAppInstalled(TEL_P_NAME) && settings.isExtDialogCancelled) {
            installExtensionDialog().show()
        }
        try {
            settings.isShowTouches =
                android.provider.Settings.System.getInt(
                    activity!!.contentResolver,
                    getString(R.string.key_show_touches)
                ) == 1
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_show_touches))!!.isChecked = settings.isShowTouches
        } catch (e: android.provider.Settings.SettingNotFoundException) {
            activity!!.container.snackbar(getString(R.string.msg_error)).anchorView = activity!!.navigation
        }
    }

    override fun onPause() {
        super.onPause()
        dialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (requestCode == RC_OPEN_TEL) { //Open TouchEnablerLegacy
                super.onActivityResult(requestCode, resultCode, data)
                when (resultCode) {
                    1 -> { //Result OK
                        activity!!.container.snackbar(getString(R.string.msg_done)).anchorView = activity!!.navigation
                        /* if (interstitialAd.isLoaded) {
                             interstitialAd.show()
                         }*/
                    }
                    2 -> { //Write Setting Permission not Granted
                        activity!!.container.snackbar(getString(R.string.msg_secure_settings_permission))
                            .setAction(getString(R.string.text_action_grant)) {
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
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

    private lateinit var loadingDialog: MaterialDialog
    private fun setUpBilling() {
        loadingDialog = context!!.showStaticProgressDialog(getString(R.string.text_please_wait))
        loadingDialog.show()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                activity!!.container.snackbar("Something went wrong.").anchorView = activity!!.navigation
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadAllSku()
                }
            }
        })
    }

    private fun loadAllSku() {
        val skuModel = Gson().fromJson(firebaseRemoteConfig.getString("pr_sku_list"), SkuModel::class.java)
        if (billingClient.isReady) {
            val params = SkuDetailsParams.newBuilder().setSkusList(skuModel.sku).setType(BillingClient.SkuType.INAPP).build()
            billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList.isNotEmpty()) {
                    Handler().postDelayed({
                        loadingDialog.dismiss()
                    }, 100)

                    val list = ArrayList<String>()
                    for (skuDetails in skuDetailsList) {
                        list.add("${skuDetails.price} - ${skuDetails.title.substringBefore("(")}")
                    }
                    MaterialDialog(context!!, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                        listItems(items = list) { _, index, _ ->
                            val billingFlowParams =
                                BillingFlowParams.newBuilder().setSkuDetails(skuDetailsList[index]).build()
                            billingClient.launchBillingFlow(activity!!, billingFlowParams)
                        }
                        title(R.string.pref_title_donate_dev)
                        negativeButton(android.R.string.cancel)
                    }
                }
            }
        } else Log.d(TAG, "loadAllSku: Billing not ready")
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}