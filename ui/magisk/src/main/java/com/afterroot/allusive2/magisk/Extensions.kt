/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import com.topjohnwu.superuser.Shell

fun reboot(reason: String = "") {
  Shell.su("/system/bin/svc power reboot $reason || /system/bin/reboot $reason").submit()
}

fun softReboot() {
  Shell.su("busybox killall system_server || busybox killall zygote").submit()
}
