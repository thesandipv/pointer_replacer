/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afterroot.allusive2.R
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.ui.OnboardingActivity
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.ui.common.compose.theme.Theme
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.getMaterialColor
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
  private lateinit var fabApply: ExtendedFloatingActionButton
  private val sharedViewModel: MainSharedViewModel by activityViewModels()

  @Inject lateinit var db: FirebaseFirestore

  @Inject lateinit var firebaseUtils: FirebaseUtils
  private lateinit var user: FirebaseUser

  private val nameState = MutableStateFlow("")

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    fabApply = requireActivity().findViewById(R.id.fab_apply)
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        Theme {
          val name by nameState.collectAsStateWithLifecycle()
          EditProfileScreen(
            username = name,
            email = if (::user.isInitialized) user.email.orEmpty() else "",
            onUsernameChange = { nameState.value = it },
          )
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (firebaseUtils.isUserSignedIn) {
      user = firebaseUtils.firebaseUser!!
      nameState.value = user.displayName.orEmpty()

      fabApply.apply {
        setOnClickListener {
          val newName = nameState.value.trim()
          if (user.displayName != newName) {
            val request = UserProfileChangeRequest.Builder()
              .setDisplayName(newName)
              .build()
            user.updateProfile(request).addOnCompleteListener { task ->
              if (task.isSuccessful) {
                sharedViewModel.displayMsg(
                  getString(CommonR.string.msg_profile_updated),
                )
                db.collection(DatabaseFields.COLLECTION_USERS)
                  .document(user.uid)
                  .update(DatabaseFields.FIELD_NAME, newName)
              }
            }
          } else {
            sharedViewModel.displayMsg(getString(CommonR.string.msg_no_changes))
          }
        }
        icon = requireContext().getDrawableExt(
          CommonR.drawable.ic_action_save,
          getMaterialColor(com.google.android.material.R.attr.colorOnSecondary),
        )
      }
    } else {
      startActivity(Intent(this.context, OnboardingActivity::class.java))
    }
  }
}

@Composable
fun EditProfileScreen(
  username: String,
  email: String,
  onUsernameChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedTextField(
      value = username,
      onValueChange = onUsernameChange,
      label = { Text(stringResource(CommonR.string.input_hint_name)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = email,
      onValueChange = {},
      label = { Text(stringResource(CommonR.string.input_hint_email)) },
      enabled = false,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
  }
}
