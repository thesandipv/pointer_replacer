/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
package com.afterroot.allusive2.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class PointerRequest(
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
  @field:JvmField var isRequestClosed: Boolean = false,
)
