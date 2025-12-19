/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data.mapper

import com.afterroot.allusive2.model.LocalUser
import com.afterroot.data.model.NetworkUser
import com.google.firebase.firestore.DocumentSnapshot

fun LocalUser.toNetworkUser() = NetworkUser(
  name = name,
  email = email,
  uid = uid,
  fcmId = fcmId,
  userName = userName,
  properties = properties,
)

fun NetworkUser.toLocalUser() = LocalUser(
  name = name,
  email = email,
  uid = uid ?: "",
  fcmId = fcmId ?: "",
  userName = userName,
  properties = properties,
)

fun DocumentSnapshot.toNetworkUser() = toObject(NetworkUser::class.java)
