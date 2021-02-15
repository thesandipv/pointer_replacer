/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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

package com.afterroot.allusive2.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.afterroot.allusive2.R
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.databinding.FragmentEditProfileBinding
import com.afterroot.allusive2.ui.SplashActivity
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.core.extensions.getDrawableExt
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.android.inject

class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var fabApply: ExtendedFloatingActionButton
    private val sharedViewModel: MainSharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        fabApply = requireActivity().findViewById(R.id.fab_apply)
        return binding.root
    }

    private val db: FirebaseFirestore by inject()
    private lateinit var user: FirebaseUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseUtils.isUserSignedIn) {
            user = FirebaseUtils.auth!!.currentUser!!
            with(view) {
                with(binding) {
                    inputProfileName.setText(user.displayName)
                    inputEmail.setText(user.email)
                    inputEmail.isEnabled = false
                }
                fabApply.apply {
                    setOnClickListener {
                        val newName = binding.inputProfileName.text.toString().trim()
                        if (user.displayName != newName) {
                            val request = UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build()
                            user.updateProfile(request).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    sharedViewModel.displayMsg(getString(R.string.msg_profile_updated))
                                    db.collection(DatabaseFields.COLLECTION_USERS)
                                        .document(user.uid)
                                        .update(DatabaseFields.FIELD_NAME, newName)
                                }
                            }
                        } else sharedViewModel.displayMsg(getString(R.string.msg_no_changes))
                    }
                    icon = requireContext().getDrawableExt(R.drawable.ic_action_save, R.color.color_on_secondary)
                }
            }
        } else {
            startActivity(Intent(this.context, SplashActivity::class.java))
        }
    }
}