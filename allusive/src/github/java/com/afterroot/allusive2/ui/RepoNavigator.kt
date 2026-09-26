/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.afterroot.allusive2.R
import com.afterroot.allusive2.ui.fragment.PointersRepoFragmentDirections

object RepoNavigator {
  fun installRro(fragment: Fragment, docId: String, fileName: String) {
    if (fragment.findNavController().currentDestination?.id == R.id.repoFragment) {
      val directions = PointersRepoFragmentDirections.repoToRroInstall(
        repoDocId = docId,
        pointerFileName = fileName,
      )
      fragment.findNavController().navigate(directions)
    }
  }
}
