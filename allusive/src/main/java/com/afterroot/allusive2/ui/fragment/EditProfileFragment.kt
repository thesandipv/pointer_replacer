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
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.lifecycleScope
import com.afterroot.allusive2.R
import com.afterroot.allusive2.data.deletedUsers
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.data.requests
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.ui.OnboardingActivity
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.ui.common.compose.theme.Theme
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.showStaticProgressDialog
import com.afterroot.utils.extensions.updateProgressText
import com.afterroot.utils.getMaterialColor
import com.firebase.ui.auth.AuthUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
  private lateinit var fabApply: ExtendedFloatingActionButton
  private val sharedViewModel: MainSharedViewModel by activityViewModels()

  @Inject lateinit var db: FirebaseFirestore

  @Inject lateinit var firebaseUtils: FirebaseUtils

  @Inject lateinit var storage: FirebaseStorage
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
            onDeleteAccountClick = { showDeleteAccountDialog() },
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

  private fun showDeleteAccountDialog() {
    MaterialAlertDialogBuilder(requireContext())
      .setTitle(getString(CommonR.string.dialog_title_delete_account))
      .setMessage(getString(CommonR.string.dialog_delete_account_confirm))
      .setPositiveButton(getString(CommonR.string.button_delete_account)) { _, _ ->
        deleteAccount()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun deleteAccount() {
    val progressDialog = requireContext().showStaticProgressDialog(
      getString(CommonR.string.text_progress_init),
    )
    progressDialog.show()

    lifecycleScope.launch(Dispatchers.IO) {
      val uid = user.uid

      // 1. Delete user's requests from Firestore
      try {
        progressDialog.updateProgressText(getString(CommonR.string.text_progress_deleting_requests))
        val requestsSnapshot = db.requests()
          .whereEqualTo(DatabaseFields.FIELD_UID, uid)
          .get()
          .await()

        for (doc in requestsSnapshot.documents) {
          doc.reference.delete().await()
        }
      } catch (e: Exception) {
        Timber.e(e, "Error deleting user requests")
      }

      // 2. Delete user's pointers from Firestore and Storage
      try {
        progressDialog.updateProgressText(getString(CommonR.string.text_progress_deleting_pointers))
        val pointersSnapshot = db.pointers().get().await()

        for (doc in pointersSnapshot.documents) {
          val pointer = doc.toObject(Pointer::class.java)
          if (pointer?.uploadedBy?.containsKey(uid) == true) {
            doc.reference.delete().await()
            pointer.filename?.let { filename ->
              try {
                storage.pointers().child(filename).delete().await()
              } catch (e: Exception) {
                Timber.e(e, "Error deleting storage pointer file: $filename")
              }
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Error deleting user pointers")
      }

      // 3. Keep record of deleted UID in 'deleted_users' collection
      try {
        progressDialog.updateProgressText(getString(CommonR.string.text_progress_deleting_profile))
        val deletedRecord = hashMapOf<String, Any>(
          DatabaseFields.FIELD_UID to uid,
          DatabaseFields.FIELD_DELETED_AT to com.google.firebase.Timestamp.now(),
        )
        db.deletedUsers().document(uid).set(deletedRecord).await()
      } catch (e: Exception) {
        Timber.e(e, "Error recording deleted UID in deleted_users: $uid")
      }

      // 4. Delete user profile document
      try {
        db.collection(DatabaseFields.COLLECTION_USERS).document(uid).delete().await()
      } catch (e: Exception) {
        Timber.e(e, "Error deleting user profile document")
      }

      // 5. Delete Firebase Auth user
      withContext(Dispatchers.Main) {
        AuthUI.getInstance().delete(requireContext()).addOnCompleteListener { task ->
          progressDialog.dismiss()
          if (task.isSuccessful) {
            Toast.makeText(
              requireContext(),
              getString(CommonR.string.dialog_delete_account_success),
              Toast.LENGTH_SHORT,
            ).show()
            val intent = Intent(requireContext(), OnboardingActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
          } else {
            Toast.makeText(
              requireContext(),
              getString(CommonR.string.dialog_delete_account_failed),
              Toast.LENGTH_LONG,
            ).show()
          }
        }
      }
    }
  }
}

@Composable
fun EditProfileScreen(
  username: String,
  email: String,
  onUsernameChange: (String) -> Unit,
  onDeleteAccountClick: () -> Unit,
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
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(
      onClick = onDeleteAccountClick,
      colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
      ),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(CommonR.string.button_delete_account))
    }
  }
}
