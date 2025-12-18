/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.model

import androidx.annotation.Keep

/**
 * Data class for Sku List Json response
 */
@Keep
data class SkuModel(val sku: List<String>)
