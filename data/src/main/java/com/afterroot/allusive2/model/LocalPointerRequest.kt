/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class LocalPointerRequest(
  /**
   * File name of pointer with extension.
   */
  var fileName: String? = null,
  /**
   * Uid of user who has requested.
   */
  var uid: String? = null,
  /**
   * Timestamp of request.
   */
  @ServerTimestamp var timestamp: Date = Timestamp.now().toDate(),
  /**
   * If true, rro will be built even if it is already exist.
   */
  var force: Boolean = false,
  /**
   * If true, rro will not be built.
   */
  var exclude: Boolean = false,
  /**
   * Id of pointer document.
   */
  var documentId: String? = null,
  /**
   * If true, request is closed.
   */
  var isRequestClosed: Boolean = false,
  var pointerName: String? = null,
)
