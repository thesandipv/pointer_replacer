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
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.data.stub.createStubPointers
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.model.SkuModel
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.utils.extensions.showStaticProgressDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.querySkuDetails
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import com.afterroot.allusive2.resources.R as CommonR

@SuppressLint("ValidFragment")
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var billingClient: BillingClient
    @Inject lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    @Inject lateinit var settings: Settings
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var firebaseUtils: FirebaseUtils
    private val sharedViewModel: MainSharedViewModel by activityViewModels()

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
        setRateOnGPlay()
        setDebugPreferences()
    }

    private fun initBilling() {
        billingClient =
            BillingClient.newBuilder(requireContext()).enablePendingPurchases().setListener { _, purchases ->
                val purchase = purchases?.first()
                if (purchase != null) { // Consume every time after successful purchase
                    val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    billingClient.consumeAsync(params) { result, _ ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            Timber.tag(TAG).d("initBilling: Purchase Done and Consumed")
                        } else Timber.tag(TAG).d("initBilling: Purchase Done but not Consumed.")
                    }
                }
            }.build()
    }

    private fun initFirebaseConfig() {
        sharedViewModel.savedStateHandle.getLiveData<Boolean>(MainSharedViewModel.KEY_CONFIG_LOADED).observe(requireActivity()) {
            if (!it) return@observe
            setDonatePref(it)
        }
    }

    private fun setDonatePref(isEnable: Boolean) {
        findPreference<Preference>(getString(CommonR.string.key_pref_donate))?.apply {
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
                    getString(CommonR.string.str_format_version),
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
                    getString(CommonR.string.theme_device_default) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    getString(CommonR.string.theme_battery) -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    getString(CommonR.string.theme_light) -> AppCompatDelegate.MODE_NIGHT_NO
                    getString(CommonR.string.theme_dark) -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            return@setOnPreferenceChangeListener true
        }
    }

    @SuppressLint("CheckResult")
    private fun setMaxPointerPaddingPref() {
        findPreference<Preference>(getString(CommonR.string.key_maxPaddingSize))!!.apply {
            summary = settings.maxPointerPadding.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                    title(res = CommonR.string.text_max_padding_size)
                    input(
                        hint = getString(CommonR.string.input_hint_max_padding_size),
                        prefill = settings.maxPointerPadding.toString(),
                        allowEmpty = false, maxLength = 3,
                        inputType = InputType.TYPE_CLASS_NUMBER
                    ) { _, input ->
                        if (input.toString().toInt() > 0) {
                            settings.maxPointerPadding = input.toString().toInt()
                            this@apply.summary = input
                        } else {
                            sharedViewModel.displayMsg(String.format(getString(CommonR.string.str_format_value_error), 0))
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

    @SuppressLint("CheckResult")
    private fun setMaxPointerSizePref() {
        findPreference<Preference>(getString(CommonR.string.key_maxPointerSize))!!.apply {
            summary = settings.maxPointerSize.toString()
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                    title(res = CommonR.string.text_max_pointer_size)
                    input(
                        hintRes = CommonR.string.text_max_pointer_size,
                        prefill = settings.maxPointerSize.toString(),
                        inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 3, allowEmpty = false
                    ) { _, input ->
                        if (input.toString().toInt() > context.getMinPointerSize()) {
                            settings.maxPointerSize = input.toString().toInt()
                            this@apply.summary = input
                        } else {
                            sharedViewModel.displayMsg(
                                String.format(getString(CommonR.string.str_format_value_error), context.getMinPointerSize())
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

    private fun setRateOnGPlay() {
        preferenceScreen.findPreference<Preference>(getString(CommonR.string.key_rate_on_g_play))!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.ITEM_NAME,
                        getString(CommonR.string.key_rate_on_g_play)
                    )
                    FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, this)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(CommonR.string.url_play_store_app_page))
                    startActivity(this)
                }
                true
            }
    }

    private lateinit var loadingDialog: MaterialDialog
    private fun setUpBilling() {
        loadingDialog = requireContext().showStaticProgressDialog(getString(CommonR.string.text_please_wait))
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
        if (!billingClient.isReady) {
            Timber.tag(TAG).d("loadAllSku: Billing not ready")
            return
        }

        lifecycleScope.launch {
            val skuModel = Gson().fromJson(firebaseRemoteConfig.getString("pr_sku_list"), SkuModel::class.java)
            val params = SkuDetailsParams.newBuilder().setSkusList(skuModel.sku).setType(BillingClient.SkuType.INAPP).build()

            val queryResult = billingClient.querySkuDetails(params)
            val billingResult = queryResult.billingResult
            val skuDetailsList = queryResult.skuDetailsList
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK && skuDetailsList == null) {
                this.cancel()
                return@launch
            }
            val list = ArrayList<String>()
            for (skuDetails in skuDetailsList!!) {
                list.add("${skuDetails.price} - ${skuDetails.title.substringBefore("(")}")
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
            withContext(Dispatchers.Main) {
                delay(100)
                loadingDialog.dismiss()
                MaterialAlertDialogBuilder(requireContext()).setTitle(getString(CommonR.string.pref_title_donate_dev))
                    .setAdapter(adapter) { _, which ->
                        val billingFlowParams =
                            BillingFlowParams.newBuilder().setSkuDetails(skuDetailsList[which]).build()
                        billingClient.launchBillingFlow(requireActivity(), billingFlowParams)
                    }.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }
    }

    private fun setDebugPreferences() {
        if (!BuildConfig.DEBUG) return
        findPreference<SwitchPreference>("key_enable_emulator")?.apply {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                Toast.makeText(requireContext(), "App will close. You'll need to restart.", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
                true
            }
        }
        findPreference<Preference>("key_create_stub_pointers")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                createStubPointers(firestore, firebaseUtils)
                true
            }
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
