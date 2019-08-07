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

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afterroot.allusive.Constants.RC_PICK_IMAGE
import com.afterroot.allusive.R
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.dbInstance
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.getDrawableExt
import com.afterroot.allusive.utils.loadBitmapFromView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.android.synthetic.main.fragment_new_pointer_post.*
import org.jetbrains.anko.design.snackbar
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class NewPointerPost : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val _tag = "NewPointerPost"
    private var isPointerImported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FirebaseStorage.getInstance()
        db = dbInstance
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_pointer_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        action_upload.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, RC_PICK_IMAGE)
        }

        activity!!.fab_apply.apply {
            setOnClickListener {
                if (verifyData()) {
                    upload(saveTmpPointer())
                }
            }
            icon = context!!.getDrawableExt(R.drawable.ic_action_apply)
        }
    }

    //Handle retrieved image uri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            isPointerImported = true
            data?.data?.also { uri ->
                Glide.with(this).load(uri).override(128).into(pointer_thumb)
            }
        }
    }

    private fun upload(file: File) {
        val dialog = MaterialDialog(context!!).show {
            customView(R.layout.dialog_progress)
            cornerRadius(16f)
            cancelable(false)
        }
        val customView = dialog.getCustomView()

        customView.text_progress.text = getString(R.string.text_progress_init)

        val storageRef = storage.reference
        val fileUri = Uri.fromFile(file)
        val fileRef = storageRef.child("pointers/${fileUri.lastPathSegment!!}")
        val uploadTask = fileRef.putFile(fileUri)

        uploadTask.addOnProgressListener {
            val progress = "${(100 * it.bytesTransferred) / it.totalByteCount}%"
            customView.text_progress.text = String.format("%s..%s", getString(R.string.text_progress_uploading), progress)
        }.addOnCompleteListener { task ->
            val map = hashMapOf<String, String>()
            map[FirebaseUtils.auth!!.uid!!] = FirebaseUtils.firebaseUser!!.displayName.toString()
            if (task.isSuccessful) {
                customView.text_progress.text = getString(R.string.text_progress_finishing_up)
                val downloadUri = task.result
                Log.d(_tag, "upload: $downloadUri")
                val pointer = Pointer(
                    edit_name.text.toString().trim(),
                    fileUri.lastPathSegment!!,
                    edit_desc.text.toString().trim(),
                    map,
                    Date()
                )
                db.collection(DatabaseFields.COLLECTION_POINTERS).add(pointer).addOnSuccessListener {
                    activity!!.apply {
                        container.snackbar(getString(R.string.msg_pointer_upload_success)).anchorView = activity!!.navigation
                        dialog.dismiss()
                        fragment_repo_nav.findNavController().navigateUp()
                    }
                }
            } else {
                container.snackbar(getString(R.string.msg_error)).anchorView = activity!!.navigation
            }
        }
    }

    /**
     * @throws IOException exception
     */
    @Throws(IOException::class)
    private fun saveTmpPointer(): File {
        val bitmap = loadBitmapFromView(pointer_thumb)
        val file = File.createTempFile("pointer", ".png", context!!.cacheDir)
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return file
    }

    private fun verifyData(): Boolean {
        return when {
            edit_name.text!!.isEmpty() -> {
                edit_name.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        input_name.isErrorEnabled = false
                    }

                })
                input_name.error = getString(R.string.msg_input_error_name_empty)
                false
            }
            edit_desc.text!!.isEmpty() -> {
                edit_desc.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        input_desc.isErrorEnabled = false
                    }

                })
                input_desc.error = getString(R.string.msg_input_error_description_empty)
                false
            }
            !isPointerImported -> {
                activity!!.container.snackbar(getString(R.string.msg_pointer_not_imported)).anchorView =
                    activity!!.navigation
                false
            }
            else -> true
        }
    }
}
