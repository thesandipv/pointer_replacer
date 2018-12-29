/*
 * Copyright (C) 2016-2018 Sandip Vaghela
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
import com.afterroot.allusive.R
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

//Fragment which acts as holder for other fragments.
/*class RepoHolderFragment : Fragment() {

    private val TAG = "RepoHolderFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_repo_holder, container, false)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu: Menu Created")
        inflater.inflate(R.menu.menu_dashboard_activity, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(findNavController(fragment_repo_nav)) || super.onOptionsItemSelected(item)
    }
}*/

class PointersRepoFragment : Fragment() {

    var auth = FirebaseAuth.getInstance()!!
    val TAG = "PointersRepoFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pointer_repo, container, false)
    }

    override fun onResume() {
        super.onResume()

        if (auth.currentUser == null) {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()))
                    .build(), rcSignIn)
        }
    }

    companion object {
        const val rcSignIn = 468
    }
}
