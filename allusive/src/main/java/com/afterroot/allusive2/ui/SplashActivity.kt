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

package com.afterroot.allusive2.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive2.Constants.RC_LOGIN
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.utils.showNetworkDialog
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import org.jetbrains.anko.browse
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashActivity : AppCompatActivity() {

    private val _tag = "SplashActivity"
    private val networkViewModel: NetworkViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = get<Settings>().theme
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                getString(R.string.theme_device_default) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                getString(R.string.theme_battery) -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                getString(R.string.theme_light) -> AppCompatDelegate.MODE_NIGHT_NO
                getString(R.string.theme_dark) -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setUpNetworkObserver()
        when {
            get<FirebaseAuth>().currentUser == null -> {
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
    }

    private fun tryLogin() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.ic_launch_screen)
                .setTosAndPrivacyPolicyUrls("", getString(R.string.url_privacy_policy))
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.EmailBuilder().setRequireName(true).build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                    )
                ).build(), RC_LOGIN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                launchDashboard()
            } else {
                Toast.makeText(this, getString(R.string.msg_login_failed), Toast.LENGTH_SHORT).show()
                tryLogin()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun launchDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private var dialog: MaterialDialog? = null
    private fun setUpNetworkObserver() {
        networkViewModel.monitor(this, onConnect = {
            if (dialog != null && dialog?.isShowing!!) dialog?.dismiss()
        }, onDisconnect = {
            dialog = showNetworkDialog(state = it, positive = { setUpNetworkObserver() }, negative = { finish() })
        })
    }
}
