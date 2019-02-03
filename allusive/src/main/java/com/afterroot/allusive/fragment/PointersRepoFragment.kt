/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.allusive.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.afterroot.allusive.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_pointer_repo.*

class PointersRepoFragment : Fragment() {

    var auth = FirebaseAuth.getInstance()!!
    val TAG = "PointersRepoFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pointer_repo, container, false)
    }

    override fun onResume() {
        super.onResume()

        fab_new_pointer_post.setOnClickListener {
            it.findNavController().navigate(R.id.new_post_dest)
        }
    }

    companion object {
        const val rcSignIn = 468
    }
}
