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
import com.afterroot.allusive.utils.DatabaseFields
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar

class EditProfileFragment : Fragment() {

    //private var listener: OnSaveButtonClick? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    var user: FirebaseUser? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.toolbar.title = "Edit Profile"

        if (FirebaseUtils.isUserSignedIn) {
            user = FirebaseUtils.auth!!.currentUser
            with(view) {
                input_profile_name.setText(user!!.displayName)
                input_email.setText(user!!.email)
                input_email.isEnabled = false
                button_save_profile.setOnClickListener { it ->
                    if (user!!.displayName != input_profile_name.text.toString()) {
                        val request = UserProfileChangeRequest.Builder()
                                .setDisplayName(input_profile_name.text.toString())
                                .build()
                        user!!.updateProfile(request).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                db.collection(DatabaseFields.USERS)
                                        .document(FirebaseUtils.auth!!.currentUser!!.uid)
                                        .set(User(input_profile_name.text.toString(),
                                                user!!.email!!,
                                                user!!.uid))
                                        .addOnSuccessListener {
                                            activity!!.view_pager.snackbar("Profile Updated")
                                        }
                            }
                        }
                    } else activity!!.view_pager.snackbar("No Changes to Save.")
                    //listener!!.onSaveButtonClicked()
                }
            }
        } else {
            //Not Logged In
        }
    }

    companion object {
        fun newInstance() = EditProfileFragment()
    }
}