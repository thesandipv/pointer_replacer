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
import com.afollestad.materialdialogs.customview.getCustomView
import com.afterroot.allusive.Constants.RC_PICK_IMAGE
import com.afterroot.allusive.R
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.dbInstance
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.getDrawableExt
import com.afterroot.allusive.utils.loadBitmapFromView
import com.afterroot.allusive.utils.showStaticProgressDialog
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
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

class NewPointerPost : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val _tag = "NewPointerPost"
    private var isPointerImported = false
    private val pointerName: String get() = edit_name.text.toString().trim()
    private val pointerDescription: String get() = edit_desc.text.toString().trim()

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
                Glide.with(this).load(uri).override(128, 128).into(pointer_thumb)
                pointer_thumb.background = context?.getDrawableExt(R.drawable.transparent_grid)
            }
        }
    }

    private fun upload(file: File) {
        val dialog = context!!.showStaticProgressDialog(getString(R.string.text_progress_init))
        val customView = dialog.getCustomView()

        val storageRef = storage.reference
        val fileUri = Uri.fromFile(file)
        val fileRef = storageRef.child("${DatabaseFields.COLLECTION_POINTERS}/${fileUri.lastPathSegment!!}")
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
                    pointerName,
                    fileUri.lastPathSegment!!,
                    pointerDescription,
                    map,
                    Timestamp.now().toDate()
                )
                db.collection(DatabaseFields.COLLECTION_POINTERS).add(pointer).addOnSuccessListener {
                    activity!!.apply {
                        container.snackbar(getString(R.string.msg_pointer_upload_success)).anchorView = activity!!.navigation
                        dialog.dismiss()
                        fragment_repo_nav.findNavController().navigateUp()
                    }
                }
            }
        }.addOnFailureListener {
            container.snackbar(getString(R.string.msg_error)).anchorView = activity!!.navigation
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
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
        }
        return file
    }

    private fun verifyData(): Boolean {
        var isOK = true

        if (pointerName.isEmpty()) {
            setListener(edit_name, input_name)
            setError(input_name)
            isOK = false
        }

        if (pointerDescription.isEmpty()) {
            setListener(edit_desc, input_desc)
            setError(input_desc)
            isOK = false
        }

        if (pointerDescription.length >= input_desc.counterMaxLength) {
            input_desc.error = "Maximum Characters"
            isOK = false
        }

        if (!isPointerImported) {
            activity!!.container.snackbar(getString(R.string.msg_pointer_not_imported)).anchorView =
                activity!!.navigation
            isOK = false
        }

        return isOK
    }

    private fun setError(inputLayoutView: TextInputLayout) {
        inputLayoutView.apply {
            isErrorEnabled = true
            error = String.format(getString(R.string.format_text_empty_error), inputLayoutView.hint)
        }
    }

    private fun setListener(editTextView: TextInputEditText, inputLayoutView: TextInputLayout) {
        editTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                inputLayoutView.isErrorEnabled = false
            }
        })
    }
}
