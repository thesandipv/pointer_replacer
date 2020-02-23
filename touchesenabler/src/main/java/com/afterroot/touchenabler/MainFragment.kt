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

package com.afterroot.touchenabler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.core.extensions.showStaticProgressDialog
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import java.io.File

class MainFragment : PreferenceFragmentCompat() {

    private lateinit var billingClient: BillingClient
    private lateinit var config: FirebaseRemoteConfig
    private lateinit var donatePreference: Preference
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var interstitialAd: InterstitialAd
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var showTouchPref: SwitchPreferenceCompat
    private val _tag: String = "TouchEnabler"
    private val isDisableAds: Boolean get() = sharedPreferences.getBoolean(getString(R.string.key_disable_ads), false)
    private var dialog: AlertDialog? = null

    private val currentSetting
        get() = Settings.System.getInt(activity!!.contentResolver, getString(R.string.key_show_touches))


    @SuppressLint("CommitPrefEdits")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        editor = sharedPreferences.edit()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this.activity!!)

        prefShowTouches()
        prefOthers()
        initBilling()

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
            .addOnCompleteListener(this.activity!!) { task ->
                if (task.isSuccessful) {
                    config.activate()
                    prefVersion()
                    setUpAds()
                    prefDonate(true)
                } else {
                    prefDonate(false)
                    Log.d(_tag, "onCreate: Error getting RemoteConfig")
                }
            }
    }

    //[Billing Start]
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

    suspend fun queryPurchaseHistory() {
        val purchaseHistoryResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchaseHistory(BillingClient.SkuType.INAPP)
        }
        if (purchaseHistoryResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            purchaseHistoryResult.purchaseHistoryRecordList!!.isNotEmpty()
        ) {
            purchaseHistoryResult.purchaseHistoryRecordList!!.forEach { purchaseHistoryRecord: PurchaseHistoryRecord ->
                val params = ConsumeParams.newBuilder().setPurchaseToken(purchaseHistoryRecord.purchaseToken).build()
                billingClient.consumeAsync(params) { result, purchaseToken ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK && purchaseToken != null) {
                        Log.d(TAG, "queryPurchaseHistory: Purchase Consumed")
                    } else Log.d(TAG, "queryPurchaseHistory: Purchase not Consumed.")
                }
            }
        }
    }

    private lateinit var loadingDialog: MaterialDialog
    private fun setUpBilling() {
        loadingDialog = context!!.showStaticProgressDialog(getString(R.string.text_please_wait))
        loadingDialog.show()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                activity!!.container.snackbar("Something went wrong.")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    GlobalScope.launch {
                        loadAllSku()
                        queryPurchaseHistory()
                    }
                }
            }
        })
    }

    suspend fun loadAllSku() {
        if (billingClient.isReady) {
            val skuModel = Gson().fromJson(config.getString("touches_sku_list"), SkuModel::class.java)
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuModel.sku).setType(BillingClient.SkuType.INAPP)
            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(params.build())
            }
            if (skuDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                skuDetailsResult.skuDetailsList!!.isNotEmpty()
            ) {
                withContext(Dispatchers.Default) {
                    delay(100)
                    loadingDialog.dismiss()
                }

                val list = ArrayList<String>()
                for (skuDetails in skuDetailsResult.skuDetailsList!!) {
                    list.add("${skuDetails.price} - ${skuDetails.title.substringBefore("(")}")
                }
                withContext(Dispatchers.Main) {
                    MaterialDialog(context!!, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                        listItems(items = list) { _, index, _ ->
                            val billingFlowParams =
                                BillingFlowParams.newBuilder().setSkuDetails(skuDetailsResult.skuDetailsList!![index])
                                    .build()
                            billingClient.launchBillingFlow(activity!!, billingFlowParams)
                        }
                        title(R.string.pref_title_donate_dev)
                        negativeButton(android.R.string.cancel)
                    }

                }
            }
        } else Log.d(TAG, "loadAllSku: Billing not ready")

    }

/*
    private fun loadAllSku() {
        val skuModel = Gson().fromJson(config.getString("touches_sku_list"), SkuModel::class.java)
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
*/

    data class SkuModel(val sku: List<String>)

    //Donate Preference
    private fun prefDonate(isEnable: Boolean) {
        //Donate Preference
        donatePreference = findPreference(getString(R.string.key_pref_donate))!!
        donatePreference.apply {
            isEnabled = isEnable
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                setUpBilling()
                return@OnPreferenceClickListener true
            }
        }
    }
    //[/Billing End]

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
                putExtra("com.afterroot.toucherlegacy.EXTRA_TOUCH_VAL", if (newValue == true) 1 else 0)
            }
            if (i.resolveActivity(activity!!.packageManager) != null) {
                startActivityForResult(i, RC_OPEN_TEL)
            } else {
                Toast.makeText(activity!!, getString(R.string.msg_install_extension_first), Toast.LENGTH_SHORT).show()
                installExtensionDialog().show()
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
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_developer))
                    startActivity(this)
                }
                true
            }

        //Rate On Google Play
        preferenceScreen.findPreference<Preference>(getString(R.string.key_rate_on_g_play))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.ITEM_NAME,
                        getString(R.string.key_rate_on_g_play)
                    )
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_app_page))
                    startActivity(this)
                }
                true
            }
    }

    override fun onResume() {
        super.onResume()

        if (!activity!!.isAppInstalled("com.afterroot.toucherlegacy")) {
            installExtensionDialog().show()
        }
        try {
            val showTouchesCurr = currentSetting == 1
            editor.putBoolean(getString(R.string.key_show_touches), showTouchesCurr).apply()
            showTouchPref.isChecked = showTouchesCurr
        } catch (e: Settings.SettingNotFoundException) {
            activity!!.root_layout.snackbar(getString(R.string.msg_error))
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
                        activity!!.root_layout.snackbar(getString(R.string.msg_done))
                        if (interstitialAd.isLoaded && !isDisableAds) {
                            interstitialAd.show()
                        }
                    }
                    2 -> { //Write Setting Permission not Granted
                        activity!!.root_layout.snackbar(getString(R.string.msg_secure_settings_permission))
                            .setAction(getString(R.string.action_grant)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data = Uri.parse("package:com.afterroot.toucherlegacy")
                                    startActivity(intent)
                                }

                            }
                    }
                    3 -> activity!!.root_layout.snackbar(getString(R.string.msg_error)) //Other error
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Context.isAppInstalled(pName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(pName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
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

        val watchAds =
            preferenceScreen.findPreference<Preference>(getString(R.string.key_watch_ads))!!
        watchAds.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (interstitialAd.isLoaded) {
                    interstitialAd.show()
                } else {
                    activity!!.root_layout.snackbar(getString(R.string.msg_ad_not_loaded))
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

    private fun installExtensionDialog(): AlertDialog {
        dialog = AlertDialog.Builder(activity!!).setTitle(getString(R.string.dialog_title_install_ext))
            .setMessage(getString(R.string.msg_install_ext_dialog))
            .setCancelable(false)
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->
                activity!!.finish()
            }
            .setPositiveButton(getString(R.string.dialog_button_install)) { _, _ ->
                val reference = FirebaseStorage.getInstance()
                    .reference.child("updates/tapslegacy-release.apk")
                val tmpFile = File(context!!.cacheDir, "app.apk")
                activity!!.root_layout.longSnackbar(getString(R.string.msg_downloading_ext))
                reference.getFile(tmpFile).addOnSuccessListener {
                    activity!!.root_layout.snackbar(getString(R.string.msg_ext_downloaded))
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
            }.setNeutralButton(getString(R.string.dialog_button_learn_more)) { _, _ ->
                activity!!.browse(getString(R.string.url_learn_more))
            }.create()
        return dialog as AlertDialog
    }

    companion object {
        const val ACTION_OPEN_TEL = "com.afterroot.action.OPEN_TPL"
        const val INAPP_LICENCE_KEY = "inapp_licence_key"
        const val PRODUCT_ID_KEY_1 = "product_id_1"
        const val PRODUCT_ID_KEY_2 = "product_id_2"
        const val RC_OPEN_TEL = 245
        const val REMOTE_CONFIG_LATEST_BUILD = "touch_enabler_latest_build"
        private const val TAG = "MainFragment"
    }

}