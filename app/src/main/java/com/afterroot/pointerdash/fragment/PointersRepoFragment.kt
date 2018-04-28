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

package com.afterroot.pointerdash.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import com.afterroot.pointerdash.R
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

//Fragment which acts as holder for other fragments.
class RepoHolderFragment : Fragment() {

    private val TAG = "RepoHolderFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_repo_holder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentManager!!.beginTransaction().add(R.id.root_fragment_repo, PointersRepoFragment()).commit()
    }

    override fun onDetach() {
        super.onDetach()
        menu.removeItem(editProfileMenuItem!!.itemId)
    }

    lateinit var menu: Menu
    var editProfileMenuItem : MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        this.menu = menu!!
        Log.d(TAG, "onCreateOptionsMenu: Menu Created")
        editProfileMenuItem = menu.add(getString(R.string.text_edit_profile))
    }
}

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
                    .build(), Companion.rcSignIn)
        }
    }

    companion object {
        const val rcSignIn = 468
    }
}