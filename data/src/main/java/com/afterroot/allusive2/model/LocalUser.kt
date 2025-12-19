/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.model

import androidx.annotation.Keep
import com.afterroot.data.model.UserProperties
import java.io.Serializable

// Collection 'users'
@Keep
data class LocalUser(
  var name: String? = null,
  var email: String? = null,
  var uid: String = "",
  var fcmId: String = "",
  var userName: String? = null,
  var properties: UserProperties = UserProperties(),
) : Serializable
