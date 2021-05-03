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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.Constants.RC_PICK_IMAGE
import com.afterroot.allusive2.R
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.databinding.FragmentNewPointerPostBinding
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.core.extensions.getAsBitmap
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.showStaticProgressDialog
import com.afterroot.core.extensions.updateProgressText
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import org.jetbrains.anko.find
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class NewPointerPost : Fragment() {

    private lateinit var binding: FragmentNewPointerPostBinding
    private lateinit var rewardedAd: RewardedAd
    private val db: FirebaseFirestore by inject()
    private val pointerDescription: String get() = binding.editDesc.text.toString().trim()
    private val pointerName: String get() = binding.editName.text.toString().trim()
    private val remoteConfig: FirebaseRemoteConfig by inject()
    private val sharedViewModel: MainSharedViewModel by activityViewModels()
    private val storage: FirebaseStorage by inject()
    private var adLoaded: Boolean = false
    private var clickedUpload: Boolean = false
    private var isPointerImported = false
    private val firebaseUtils: FirebaseUtils by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewPointerPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/png"
            }
            val pickIntent =
                Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/png"
                }
            val chooserIntent = Intent.createChooser(intent, "Choose Pointer Image").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
            }
            //FIXME update method https://developer.android.com/training/basics/intents/result#kotlin
            startActivityForResult(chooserIntent, RC_PICK_IMAGE)
        }

        initFirebaseConfig()
    }

    private fun initFirebaseConfig() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { result ->
            kotlin.runCatching {
                //Load Banner Ad
                val adView = AdView(requireContext())
                adView.apply {
                    adSize = AdSize.BANNER
                    adUnitId = if (BuildConfig.DEBUG || (!result.isSuccessful && BuildConfig.DEBUG)) {
                        getString(R.string.ad_banner_new_pointer)
                    } else remoteConfig.getString("ad_banner_new_pointer")
                    binding.adContainer.addView(this)
                    loadAd(AdRequest.Builder().build())
                }

                if (result.isSuccessful) {
                    if (remoteConfig.getBoolean("FLAG_ENABLE_REWARDED_ADS")) {
                        setUpRewardedAd()
                        requireActivity().find<ExtendedFloatingActionButton>(R.id.fab_apply).apply {
                            setOnClickListener {
                                if (verifyData()) {
                                    clickedUpload = true
                                    MaterialDialog(requireContext()).show {
                                        title(R.string.text_action_upload)
                                        message(R.string.dialog_msg_rewarded_ad)
                                        positiveButton(android.R.string.ok) {
/*
                                            if (rewardedAd.isLoaded) {
                                                showAd()
                                            } else {
                                                sharedViewModel.displayMsg("Ad is not loaded yet. Loading...")
                                            }
*/
                                        }
                                        negativeButton(R.string.fui_cancel)
                                    }
                                }
                            }
                            icon = requireContext().getDrawableExt(R.drawable.ic_action_apply)
                        }
                    } else {
                        setFabAsDirectUpload()
                    }
                } else {
                    setFabAsDirectUpload()
                }
            }
        }
    }

    private fun setFabAsDirectUpload() {
        requireActivity().find<ExtendedFloatingActionButton>(R.id.fab_apply).apply {
            setOnClickListener {
                if (verifyData()) {
                    upload(saveTmpPointer())
                }
            }
            icon = requireContext().getDrawableExt(R.drawable.ic_action_apply)
        }
    }

    private fun showAd() { //TODO Rewrite logic
        rewardedAd.setOnPaidEventListener {

        }

/*
        val adCallback = object : RewardedAdCallback() {
            override fun onUserEarnedReward(p0: RewardItem) {
                clickedUpload = false
                if (verifyData()) {
                    upload(saveTmpPointer())
                }
            }

            override fun onRewardedAdClosed() {
                super.onRewardedAdClosed()
                setUpRewardedAd()
            }
        }
*/
    }

    private fun createAndLoadRewardedAd() {
        val adUnitId = whenBuildIs(
            debug = getString(R.string.ad_rewarded_1_id),
            release = remoteConfig.getString("ad_rewarded_1_id")
        )
        val adLoadCallback = object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                super.onAdLoaded(ad)
                adLoaded = true
                rewardedAd = ad
                if (clickedUpload) {
                    showAd()
                }
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                adLoaded = false
            }
        }
        RewardedAd.load(requireContext(), adUnitId, AdRequest.Builder().build(), adLoadCallback)
    }

    private fun setUpRewardedAd() {
        kotlin.runCatching {
            createAndLoadRewardedAd()
        }
    }

    //Handle retrieved image uri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            isPointerImported = true
            data?.data?.also { uri ->
                Glide.with(this).load(uri).override(128, 128).centerCrop().into(binding.pointerThumb)
                binding.pointerThumb.background = context?.getDrawableExt(R.drawable.transparent_grid)
            }
        }
    }

    private fun upload(file: File) {
        val dialog = requireContext().showStaticProgressDialog(getString(R.string.text_progress_init))

        val storageRef = storage.reference
        val fileUri = Uri.fromFile(file)
        val fileRef = storageRef.child("${DatabaseFields.COLLECTION_POINTERS}/${fileUri.lastPathSegment!!}")
        val uploadTask = fileRef.putFile(fileUri)

        uploadTask.addOnProgressListener {
            val progress = "${(100 * it.bytesTransferred) / it.totalByteCount}%"
            dialog.updateProgressText(String.format("%s..%s", getString(R.string.text_progress_uploading), progress))
        }.addOnCompleteListener { task ->
            val map = hashMapOf<String, String>()
            map[firebaseUtils.uid!!] = firebaseUtils.firebaseUser?.displayName.toString()
            if (task.isSuccessful) {
                dialog.updateProgressText(getString(R.string.text_progress_finishing_up))
                val pointer = Pointer(
                    name = pointerName,
                    filename = fileUri.lastPathSegment!!,
                    description = pointerDescription,
                    uploadedBy = map,
                    time = Timestamp.now().toDate()
                )
                Log.d(TAG, "upload: $pointer")
                db.collection(DatabaseFields.COLLECTION_POINTERS).add(pointer).addOnSuccessListener {
                    requireActivity().apply {
                        sharedViewModel.displayMsg(getString(R.string.msg_pointer_upload_success))
                        dialog.dismiss()
                        findNavController().navigateUp()
                    }
                }.addOnFailureListener {
                    sharedViewModel.displayMsg(getString(R.string.msg_error))
                }
            }
        }.addOnFailureListener {
            binding.pointerThumb.background = context?.getDrawableExt(R.drawable.transparent_grid)
            sharedViewModel.displayMsg(getString(R.string.msg_error))
        }
    }

    /**
     * @throws IOException exception
     */
    @Throws(IOException::class)
    private fun saveTmpPointer(): File {
        binding.pointerThumb.background = null
        val bitmap = binding.pointerThumb.getAsBitmap()
        val file = File.createTempFile("pointer", ".png", requireContext().cacheDir)
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
        binding.apply {
            if (pointerName.isEmpty()) {
                setListener(editName, inputName)
                setError(inputName)
                isOK = false
            }

            if (pointerDescription.length >= inputDesc.counterMaxLength) {
                setListener(editDesc, inputDesc)
                inputDesc.error = "Maximum Characters"
                isOK = false
            }

            if (!isPointerImported) {
                sharedViewModel.displayMsg(getString(R.string.msg_pointer_not_imported))
                isOK = false
            }
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

    companion object {
        private const val TAG = "NewPointerPost"
    }
}
