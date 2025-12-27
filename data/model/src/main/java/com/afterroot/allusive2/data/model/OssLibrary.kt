/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OssLibrary(
  val groupId: String,
  val artifactId: String,
  val name: String = artifactId,
  val spdxLicenses: List<License>? = null,
  val unknownLicenses: List<License>? = null,
) {
  val license: License? = spdxLicenses?.firstOrNull() ?: unknownLicenses?.firstOrNull()

  @Serializable
  data class License(val name: String, val url: String)
}
