/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.database

object DatabaseFields {
  // table 'users'
  const val COLLECTION_USERS = "users"
  const val FIELD_NAME = "name"
  const val FIELD_EMAIL = "email"
  const val FIELD_UID = "uid"
  const val FIELD_FCM_ID = "fcmId"
  const val FIELD_USERNAME = "userName"
  const val FIELD_USER_PROPERTIES = "properties"
  const val FIELD_VERSION = "version"

  // table 'pointers'
  const val COLLECTION_POINTERS = "pointers"
  const val FIELD_DESC = "description"
  const val FIELD_FILENAME = "filename"
  const val FIELD_TIME = "time"
  const val FIELD_UPLOAD_BY = "uploadedBy"
  const val FIELD_DOWNLOADS = "downloads"
  const val FIELD_RRO_DOWNLOADS = "rroDownloads"
  const val FIELD_UPVOTES = "upvotes"
  const val FIELD_DOWNVOTES = "downvotes"
  const val FIELD_HAS_RRO = "hasRRO"
  const val FIELD_RRO_REQUESTED = "rroRequested"

  // table 'requests'
  const val COLLECTION_REQUESTS = "requests"
  const val FIELD_REQUEST_CLOSED = "isRequestClosed"
  const val FIELD_TIMESTAMP = "timestamp"
}
