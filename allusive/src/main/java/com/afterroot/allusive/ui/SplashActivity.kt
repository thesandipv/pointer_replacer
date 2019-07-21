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

package com.afterroot.allusive.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afterroot.allusive.R
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setLogo(R.drawable.ic_launch_screen)
                    .setTosAndPrivacyPolicyUrls("", getString(R.string.url_privacy_policy))
                    .setAvailableProviders(
                        listOf(
                            AuthUI.IdpConfig.GoogleBuilder().build(),
                            AuthUI.IdpConfig.PhoneBuilder().build()
                        )
                    ).build(), RC_LOGIN
            )
        } else {
            launchDashboard()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                launchDashboard()
            } else {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun launchDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val RC_LOGIN = 42
    }
}
