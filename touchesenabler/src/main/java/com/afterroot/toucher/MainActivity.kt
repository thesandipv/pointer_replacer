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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setTheme(R.style.Main_AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction().replace(R.id.container, MainFragment()).commit()
        val builder = AdRequest.Builder()
        val adRequest = builder.build()
        adView.loadAd(adRequest)

        MobileAds.initialize(this, getString(R.string.admob_app_id))
        if (intent.extras != null) {
            Log.d(TAG, "onCreate: ${intent?.extras?.get("link")}")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(intent?.extras?.getString("link"))
            }
            startActivity(intent)

        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}