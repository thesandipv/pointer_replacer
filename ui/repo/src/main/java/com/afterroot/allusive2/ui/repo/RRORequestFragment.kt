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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
