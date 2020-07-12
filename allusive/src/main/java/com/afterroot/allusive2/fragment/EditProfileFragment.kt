/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.allusive2.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afterroot.allusive2.R
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.ui.SplashActivity
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.core.extensions.getDrawableExt
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.inject

class EditProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    private val db: FirebaseFirestore by inject()
    private lateinit var user: FirebaseUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseUtils.isUserSignedIn) {
            user = FirebaseUtils.auth!!.currentUser!!
            with(view) {
                input_profile_name.setText(user.displayName)
                input_email.setText(user.email)
                input_email.isEnabled = false
                requireActivity().fab_apply.apply {
                    setOnClickListener {
                        val newName = this@with.input_profile_name.text.toString().trim()
                        if (user.displayName != newName) {
                            val request = UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build()
                            user.updateProfile(request).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    requireActivity().container.snackbar(getString(R.string.msg_profile_updated))
                                        .anchorView = requireActivity().navigation
                                    db.collection(DatabaseFields.COLLECTION_USERS)
                                        .document(user.uid)
                                        .update(DatabaseFields.FIELD_NAME, newName)
                                }
                            }
                        } else requireActivity().container.snackbar(getString(R.string.msg_no_changes))
                            .anchorView = requireActivity().navigation
                    }
                    icon = requireContext().getDrawableExt(R.drawable.ic_action_save, R.color.color_on_secondary)
                }
            }
        } else {
            startActivity(Intent(this.context, SplashActivity::class.java))
        }
    }
}