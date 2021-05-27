/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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

package com.afterroot.allusive2.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.model.SkuModel
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.core.extensions.showStaticProgressDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.SkuDetailsParams
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

@SuppressLint("ValidFragment")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var billingClient: BillingClient
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var interstitialAd: InterstitialAd
    private val settings: Settings by inject()
    private val sharedViewModel: MainSharedViewModel by viewModel()

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
        setVersionPref()
        setDonatePref(false)
        initBilling()
//        setUpAds()
        setRateOnGPlay()
    }

    private fun initBilling() {
        billingClient =
            BillingClient.newBuilder(requireContext()).enablePendingPurchases().setListener { _, purchases ->
                val purchase = purchases?.first()
                if (purchase != null) { // Consume every time after successful purchase
                    val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    billingClient.consumeAsync(params) { result, _ ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
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
                .addOnCompleteListener(requireActivity()) { result ->
                    try {
                        if (result.isSuccessful) {
                            firebaseRemoteConfig.activate()
                            setDonatePref(true)
                        } else {
                            setDonatePref(false)
                        }
                    } catch (ignored: IllegalStateException) {
                        // if user changed context before completing.
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
                String.format(
                    getString(R.string.str_format_version),
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.COMMIT_ID
                )
        }
    }

    private fun setOpenSourceLicPref() {
        findPreference<Preference>("licenses")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                OssLicensesMenuActivity.setActivityTitle("Licences").apply { }
                startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
                return@OnPreferenceClickListener true
            }
        }
    }

    private fun setAppThemePref() {
        findPreference<ListPreference>("key_app_theme")?.setOnPreferenceChangeListener { _, newValue ->
            AppCompatDelegate.setDefaultNightMode(
                when (newValue) {
                    getString(R.string.theme_device_default) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    getString(R.string.theme_battery) -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    getString(R.string.theme_light) -> AppCompatDelegate.MODE_NIGHT_NO
                    getString(R.string.theme_dark) -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            return@setOnPreferenceChangeListener true
        }
    }

    private fun setMaxPointerPaddingPref() {
        findPreference<Preference>(getString(R.string.key_maxPaddingSize))!!.apply {
            summary = settings.maxPointerPadding.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                    title(res = R.string.text_max_padding_size)
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
                            sharedViewModel.displayMsg(String.format(getString(R.string.str_format_value_error), 0))
                        }
                    }
                    (getInputLayout().getChildAt(0) as FrameLayout).updateLayoutParams<LinearLayout.LayoutParams> {
                        setMargins(0)
                    }
                }
                false
            }
        }
    }

    private fun setMaxPointerSizePref() {
        findPreference<Preference>(getString(R.string.key_maxPointerSize))!!.apply {
            summary = settings.maxPointerSize.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
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
                            sharedViewModel.displayMsg(
                                String.format(getString(R.string.str_format_value_error), context.getMinPointerSize())
                            )
                        }
                    }
                    (getInputLayout().getChildAt(0) as FrameLayout).updateLayoutParams<LinearLayout.LayoutParams> {
                        setMargins(0)
                    }
                }
                false
            }
        }
    }

/*
    private fun setUpAds() {
        interstitialAd = InterstitialAd(this.requireActivity())
        interstitialAd.apply {
            adUnitId = if (BuildConfig.DEBUG) {
                getString(R.string.ad_interstitial_1_id)
            } else firebaseRemoteConfig.getString("ad_interstitial_1_id")
            loadAd(AdRequest.Builder().build())
        }

        val watchAds =
            preferenceScreen.findPreference<Preference>(getString(R.string.key_watch_ads))!!
        watchAds.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (interstitialAd.isLoaded) {
                    interstitialAd.show()
                } else {
                    sharedViewModel.displayMsg(getString(R.string.msg_ad_not_loaded))
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
*/

    private fun setRateOnGPlay() {
        preferenceScreen.findPreference<Preference>(getString(R.string.key_rate_on_g_play))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.ITEM_NAME,
                        getString(R.string.key_rate_on_g_play)
                    )
                    FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_play_store_app_page))
                    startActivity(this)
                }
                true
            }
    }

    private lateinit var loadingDialog: MaterialDialog
    private fun setUpBilling() {
        loadingDialog = requireContext().showStaticProgressDialog(getString(R.string.text_please_wait))
        loadingDialog.show()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                sharedViewModel.displayMsg("Something went wrong.")
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
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList?.isNotEmpty()!!) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            loadingDialog.dismiss()
                        },
                        100
                    )

                    val list = ArrayList<String>()
                    for (skuDetails in skuDetailsList) {
                        list.add("${skuDetails.price} - ${skuDetails.title.substringBefore("(")}")
                    }
                    MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                        listItems(items = list) { _, index, _ ->
                            val billingFlowParams =
                                BillingFlowParams.newBuilder().setSkuDetails(skuDetailsList[index]).build()
                            billingClient.launchBillingFlow(requireActivity(), billingFlowParams)
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
