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
package com.afterroot.allusive2.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.utils.showNetworkDialog
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.jetbrains.anko.browse
import org.jetbrains.anko.toast
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

  private val networkViewModel: NetworkViewModel by viewModels()
  private lateinit var settings: Settings

  @Inject lateinit var firebaseAuth: FirebaseAuth

  @Inject lateinit var firestore: FirebaseFirestore

  @Inject lateinit var firebaseUtils: FirebaseUtils

  override fun onCreate(savedInstanceState: Bundle?) {
    settings = Settings(this)
    val theme = settings.theme
    AppCompatDelegate.setDefaultNightMode(
      when (theme) {
        getString(
          CommonR.string.theme_device_default,
        ),
        -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        getString(CommonR.string.theme_battery) -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        getString(CommonR.string.theme_light) -> AppCompatDelegate.MODE_NIGHT_NO
        getString(CommonR.string.theme_dark) -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      },
    )
    super.onCreate(savedInstanceState)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    setUpNetworkObserver()
    when {
      !firebaseUtils.isUserSignedIn -> {
        tryLogin()
      }
      intent.extras != null -> {
        intent.extras?.let {
          val link = it.getString("link")
          when {
            link != null -> {
              browse(link, true)
            }
            else -> {
              launchDashboard()
            }
          }
          finish()
        }
      }
      else -> {
        launchDashboard()
      }
    }

    // Use Firebase emulators
    runCatching {
      if (BuildConfig.DEBUG && settings.getBoolean("key_enable_emulator", false)) {
        firestore.useEmulator("10.0.2.2", 8080)
      }
    }
  }

  private val resultLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) {
    if (it.resultCode == Activity.RESULT_OK) {
      launchDashboard()
    } else {
      if (it.idpResponse == null) {
        toast("Sign In Cancelled")
      }

      if (it.idpResponse?.error?.errorCode == ErrorCodes.NO_NETWORK) {
        toast("No internet")
      }

      toast("Error: ${it.idpResponse?.error?.message}")
      Timber.e(it.idpResponse?.error, "Sign-in error: ${it.idpResponse?.error?.message}")

      tryLogin()
    }
  }

  private fun tryLogin() {
    val pickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_custom_auth)
      .setGoogleButtonId(R.id.button_auth_sign_in_google)
      .setEmailButtonId(R.id.button_auth_sign_in_email)
      .setTosAndPrivacyPolicyId(R.id.text_top_pp)
      .build()

    resultLauncher.launch(
      AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAuthMethodPickerLayout(pickerLayout)
        .setTheme(CommonR.style.MyTheme_Main_FirebaseUI)
        .setLogo(CommonR.drawable.ic_login_screen)
        .setTosAndPrivacyPolicyUrls(
          getString(CommonR.string.url_privacy_policy),
          getString(CommonR.string.url_privacy_policy),
        )
        .setAvailableProviders(
          listOf(
            AuthUI.IdpConfig.EmailBuilder().setRequireName(true).build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
          ),
        ).build(),
    )
  }

  private fun launchDashboard() {
    startActivity(Intent(this, MainActivity::class.java))
    finish()
  }

  private var dialog: AlertDialog? = null
  private fun setUpNetworkObserver() {
    networkViewModel.monitor(
      this,
      onConnect = {
        if (dialog != null && dialog?.isShowing!!) dialog?.dismiss()
      },
      onDisconnect = {
        dialog = showNetworkDialog(state = it, positive = {
          setUpNetworkObserver()
        }, negative = { finish() })
      },
    )
  }
}
