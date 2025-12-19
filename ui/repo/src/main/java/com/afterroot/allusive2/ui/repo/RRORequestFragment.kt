/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.afterroot.allusive2.repo.databinding.FragmentRroRequestBinding
import com.afterroot.allusive2.viewmodel.RepoViewModel
import com.afterroot.ui.common.compose.theme.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RRORequestFragment : Fragment() {

  private var _binding: FragmentRroRequestBinding? = null
  private val binding get() = _binding!!
  private val repoViewModel: RepoViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loadRequests()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    _binding = FragmentRroRequestBinding.inflate(inflater, container, false)
    val view = binding.root
    binding.composeLayout.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        Theme(requireContext()) {
          Requests()
        }
      }
    }
    return view
  }

  private fun loadRequests() {
    repoViewModel.submitAction(RepoActions.LoadRequests)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
