/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class UserSettingsSerializer @Inject constructor() : Serializer<UserSettings> {
  override val defaultValue: UserSettings
    get() = UserSettings.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): UserSettings = try {
    // readFrom is already called on the data store background thread
    UserSettings.parseFrom(input)
  } catch (exception: InvalidProtocolBufferException) {
    throw CorruptionException("Cannot read proto.", exception)
  }

  override suspend fun writeTo(t: UserSettings, output: OutputStream) {
    t.writeTo(output)
  }
}
