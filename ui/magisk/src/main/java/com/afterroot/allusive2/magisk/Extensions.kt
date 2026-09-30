/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import com.afterroot.allusive2.base.reboot as baseReboot
import com.afterroot.allusive2.base.softReboot as baseSoftReboot

fun reboot(reason: String = "") {
  baseReboot(reason)
}

fun softReboot() {
  baseSoftReboot()
}
