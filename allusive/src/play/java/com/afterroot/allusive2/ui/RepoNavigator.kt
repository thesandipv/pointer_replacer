/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.afterroot.allusive2.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.afterroot.allusive2.resources.R as CommonR

object RepoNavigator {
  fun installRro(
    fragment: Fragment,
    docId: String,
    fileName: String,
    pointerType: Int = Constants.POINTER_TOUCH,
  ) {
    val context = fragment.requireContext()
    val webUrl = "https://pointer-replacer.web.app/rro/$docId"

    MaterialAlertDialogBuilder(context)
      .setTitle(CommonR.string.text_get_rro_web)
      .setMessage(CommonR.string.dialog_msg_rro_web_info)
      .setPositiveButton(CommonR.string.text_action_browse) { _, _ ->
        val intent = Intent(Intent.ACTION_VIEW, webUrl.toUri())
        context.startActivity(intent)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
