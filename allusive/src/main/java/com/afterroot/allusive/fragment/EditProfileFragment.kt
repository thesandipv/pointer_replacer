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
import com.afterroot.allusive.R
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.model.User
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.getDrawableExt
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar

class EditProfileFragment : Fragment() {

    //private var listener: OnSaveButtonClick? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    lateinit var user: FirebaseUser
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseUtils.isUserSignedIn) {
            user = FirebaseUtils.auth!!.currentUser!!
            with(view) {
                input_profile_name.setText(user.displayName)
                input_email.setText(user.email)
                input_email.isEnabled = false
                activity!!.fab_apply.apply {
                    setOnClickListener {
                        val newName = this@with.input_profile_name.text.toString()
                        if (user.displayName != newName) {
                            val request = UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build()
                            user.updateProfile(request).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    activity!!.container.snackbar("Profile Updated")
                                    db.collection(DatabaseFields.USERS)
                                        .document(user.uid)
                                        .set(
                                            User(newName, user.email, user.uid)
                                        )
                                }
                            }
                        } else activity!!.container.snackbar("No Changes to Save.")
                        //listener!!.onSaveButtonClicked()
                    }
                    icon = context!!.getDrawableExt(R.drawable.ic_action_save, R.color.color_on_secondary)
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