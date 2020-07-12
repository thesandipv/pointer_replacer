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

package com.afterroot.allusive2.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object FirebaseUtils {
    var auth: FirebaseAuth? = null
        get() {
            Log.d("FirebaseUtils", "FirebaseUtils.auth: initializing Auth")
            return field ?: FirebaseAuth.getInstance()
        }

    val firebaseUser: FirebaseUser? = null
        get() {
            Log.d("FirebaseUtils", "FirebaseUtils.getFirebaseUser: getting user")
            return field ?: auth!!.currentUser
        }

    val isUserSignedIn: Boolean
        get() {
            if (firebaseUser == null) {
                return false
            }
            return true
        }
}