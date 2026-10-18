/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.PointerStatus
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.utils.ImageHashUtils
import com.afterroot.allusive2.utils.ModerationUtils
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.ui.common.compose.theme.Theme
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.showStaticProgressDialog
import com.afterroot.utils.extensions.updateProgressText
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class NewPointerPost : Fragment() {

  @Inject lateinit var db: FirebaseFirestore

  @Inject lateinit var firebaseUtils: FirebaseUtils

  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  @Inject lateinit var storage: FirebaseStorage

  private var rewardedAd: RewardedAd? = null
  private val sharedViewModel: MainSharedViewModel by activityViewModels()
  private var isAdLoading: Boolean = false
  private var clickedUpload: Boolean = false
  private var userEarnedReward: Boolean = false

  private val nameState = MutableStateFlow("")
  private val descState = MutableStateFlow("")
  private val imageUriState = MutableStateFlow<Uri?>(null)
  private val nameErrorState = MutableStateFlow<String?>(null)
  private val descErrorState = MutableStateFlow<String?>(null)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      Theme {
        val name by nameState.collectAsStateWithLifecycle()
        val description by descState.collectAsStateWithLifecycle()
        val imageUri by imageUriState.collectAsStateWithLifecycle()
        val nameError by nameErrorState.collectAsStateWithLifecycle()
        val descError by descErrorState.collectAsStateWithLifecycle()

        val adUnitId = if (BuildConfig.DEBUG) {
          stringResource(CommonR.string.ad_banner_new_pointer)
        } else {
          remoteConfig.getString("ad_banner_new_pointer")
        }

        val launcher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
          if (uri != null) {
            if (!isPngFormat(uri)) {
              sharedViewModel.displayMsg(getString(CommonR.string.msg_only_png_supported))
              return@rememberLauncherForActivityResult
            }
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use {
              BitmapFactory.decodeStream(it)
            }
            if (bitmap == null || !hasAlphaChannel(bitmap)) {
              sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_no_alpha_error))
              return@rememberLauncherForActivityResult
            }
            imageUriState.value = uri
          }
        }

        NewPointerPostScreen(
          name = name,
          description = description,
          imageUri = imageUri,
          nameError = nameError,
          descError = descError,
          adUnitId = adUnitId,
          onNameChange = {
            nameState.value = it
            if (nameErrorState.value != null && it.isNotBlank()) {
              nameErrorState.value = null
            }
          },
          onDescChange = {
            descState.value = it
            if (descErrorState.value != null && it.length <= 124) {
              descErrorState.value = null
            }
          },
          onBrowseClick = {
            launcher.launch("image/png")
          },
        )
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (!firebaseUtils.isUserSignedIn) {
      sharedViewModel.displayMsg(getString(CommonR.string.dialog_msg_sign_in_to_upload))
      findNavController().navigateUp()
      return
    }

    setFabAsDirectUpload()
    initFirebaseConfig()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    rewardedAd?.fullScreenContentCallback = null
    clickedUpload = false
  }

  @SuppressLint("MissingPermission")
  private fun initFirebaseConfig() {
    remoteConfig.fetchAndActivate().addOnCompleteListener { result ->
      kotlin.runCatching {
        if (!isAdded) return@addOnCompleteListener
        if (result.isSuccessful) {
          if (remoteConfig.getBoolean("FLAG_ENABLE_REWARDED_ADS")) {
            setUpRewardedAd()
            requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.fab_apply)?.apply {
              setOnClickListener {
                if (verifyData()) {
                  MaterialDialog(requireContext()).show {
                    title(CommonR.string.text_action_upload)
                    message(CommonR.string.dialog_msg_rewarded_ad)
                    positiveButton(android.R.string.ok) {
                      if (rewardedAd != null) {
                        showAd()
                      } else {
                        clickedUpload = true
                        sharedViewModel.displayMsg(getString(CommonR.string.msg_ad_loading))
                        if (!isAdLoading) {
                          setUpRewardedAd()
                        }
                      }
                    }
                    negativeButton(android.R.string.cancel)
                  }
                }
              }
              icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
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

  private fun performUpload() {
    val uri = imageUriState.value ?: return
    val result = saveTmpPointer(uri)
    upload(result.file, result.dHash)
  }

  private fun setFabAsDirectUpload() {
    requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.fab_apply)?.apply {
      setOnClickListener {
        if (verifyData()) {
          performUpload()
        }
      }
      icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
    }
  }

  private fun showAd() {
    val currentActivity = activity ?: return
    val ad = rewardedAd ?: return
    userEarnedReward = false
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        super.onAdDismissedFullScreenContent()
        rewardedAd = null
        setUpRewardedAd()
        if (userEarnedReward) {
          performUpload()
        } else {
          sharedViewModel.displayMsg(getString(CommonR.string.msg_watch_ad_to_upload))
        }
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        super.onAdFailedToShowFullScreenContent(adError)
        Timber.e("Failed to show rewarded ad: %s", adError.message)
        rewardedAd = null
        setUpRewardedAd()
        performUpload()
      }
    }
    ad.show(currentActivity) {
      userEarnedReward = true
    }
  }

  private fun createAndLoadRewardedAd() {
    val currentContext = context ?: return
    if (isAdLoading || rewardedAd != null) return
    isAdLoading = true
    val adUnitId = whenBuildIs(
      debug = getString(CommonR.string.ad_rewarded_1_id),
      release = remoteConfig.getString("ad_rewarded_1_id"),
    )
    val adLoadCallback = object : RewardedAdLoadCallback() {
      override fun onAdLoaded(ad: RewardedAd) {
        super.onAdLoaded(ad)
        isAdLoading = false
        rewardedAd = ad
        if (clickedUpload) {
          clickedUpload = false
          showAd()
        }
      }

      override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        super.onAdFailedToLoad(loadAdError)
        isAdLoading = false
        rewardedAd = null
        Timber.e("Failed to load rewarded ad: %s", loadAdError.message)
        if (clickedUpload) {
          clickedUpload = false
          performUpload()
        }
      }
    }
    RewardedAd.load(currentContext, adUnitId, AdRequest.Builder().build(), adLoadCallback)
  }

  private fun setUpRewardedAd() {
    kotlin.runCatching {
      createAndLoadRewardedAd()
    }
  }

  private fun upload(file: File, dHash: String?) {
    val dialog = requireContext()
      .showStaticProgressDialog(getString(CommonR.string.text_progress_init))

    lifecycleScope.launch {
      try {
        val sha256 = ImageHashUtils.calculateSHA256(file)

        // 1. Check user posting permissions
        val userDoc = db.collection(DatabaseFields.COLLECTION_USERS)
          .document(firebaseUtils.uid)
          .get()
          .await()
        val properties = userDoc.get(DatabaseFields.FIELD_USER_PROPERTIES) as? Map<*, *>
        val canPost = properties?.get("canPost") as? Boolean ?: true
        if (!canPost) {
          dialog.dismiss()
          sharedViewModel.displayMsg(getString(CommonR.string.msg_posting_revoked))
          return@launch
        }

        // 2. Check for duplicate SHA-256
        val duplicateSnapshot = db.collection(DatabaseFields.COLLECTION_POINTERS)
          .whereEqualTo(DatabaseFields.FIELD_SHA256, sha256)
          .limit(1)
          .get()
          .await()
        if (!duplicateSnapshot.isEmpty) {
          dialog.dismiss()
          sharedViewModel.displayMsg(getString(CommonR.string.msg_duplicate_pointer_error))
          return@launch
        }

        // 3. Upload to Firebase Storage
        val storageRef = storage.reference
        val fileUri = Uri.fromFile(file)
        val fileRef = storageRef.child(
          "${DatabaseFields.COLLECTION_POINTERS}/${fileUri.lastPathSegment!!}",
        )
        val uploadTask = fileRef.putFile(fileUri)

        uploadTask.addOnProgressListener {
          val progress = "${(100 * it.bytesTransferred) / it.totalByteCount}%"
          dialog.updateProgressText(
            String.format(
              "%s..%s",
              getString(CommonR.string.text_progress_uploading),
              progress,
            ),
          )
        }.addOnCompleteListener { task ->
          val map = hashMapOf<String, String>()
          map[firebaseUtils.uid] = firebaseUtils.firebaseUser?.displayName.toString()
          if (task.isSuccessful) {
            dialog.updateProgressText(getString(CommonR.string.text_progress_finishing_up))
            val pointer = Pointer(
              name = nameState.value.trim(),
              filename = fileUri.lastPathSegment!!,
              description = descState.value.trim(),
              uploadedBy = map,
              time = Timestamp.now().toDate(),
              status = PointerStatus.APPROVED,
              reasonCode = Reason.OK,
              sha256 = sha256,
              dHash = dHash,
            )
            Timber.tag(TAG).d("upload: %s", pointer)
            db.collection(
              DatabaseFields.COLLECTION_POINTERS,
            ).add(pointer).addOnSuccessListener {
              requireActivity().apply {
                sharedViewModel.displayMsg(
                  getString(CommonR.string.msg_pointer_upload_success),
                )
                dialog.dismiss()
                findNavController().navigateUp()
              }
            }.addOnFailureListener {
              sharedViewModel.displayMsg(getString(CommonR.string.msg_error))
              dialog.dismiss()
            }
          }
        }.addOnFailureListener {
          dialog.dismiss()
          sharedViewModel.displayMsg(getString(CommonR.string.msg_error))
        }
      } catch (e: Exception) {
        Timber.e(e, "upload failed")
        dialog.dismiss()
        sharedViewModel.displayMsg(getString(CommonR.string.msg_error))
      }
    }
  }

  private data class SavedTmpPointerResult(val file: File, val dHash: String?)

  @Throws(IOException::class)
  private fun saveTmpPointer(uri: Uri): SavedTmpPointerResult {
    val originalBitmap = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
      BitmapFactory.decodeStream(inputStream)
    } ?: throw IOException("Failed to decode bitmap from URI")

    val resizedBitmap = if (originalBitmap.width != POINTER_SIZE ||
      originalBitmap.height != POINTER_SIZE
    ) {
      originalBitmap.scale(POINTER_SIZE, POINTER_SIZE)
    } else {
      originalBitmap
    }

    val dHash = ImageHashUtils.calculateDHash(resizedBitmap)
    val file = File.createTempFile("pointer", ".png", requireContext().cacheDir)
    try {
      FileOutputStream(file).use { out ->
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
      }
    } catch (e: Exception) {
      Timber.e(e, "saveTmpPointer: Failed to compress and save temporary pointer file")
    }
    return SavedTmpPointerResult(file, dHash)
  }

  private fun isPngFormat(uri: Uri): Boolean {
    val context = requireContext()
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType == "image/png") return true

    return try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, options)
      }
      options.outMimeType == "image/png"
    } catch (e: Exception) {
      false
    }
  }

  private fun hasAlphaChannel(bitmap: Bitmap): Boolean {
    if (!bitmap.hasAlpha()) return false
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    for (pixel in pixels) {
      val alpha = (pixel ushr 24) and 0xFF
      if (alpha < 255) return true
    }
    return false
  }

  private fun verifyData(): Boolean {
    var isOK = true
    val name = nameState.value.trim()
    val desc = descState.value.trim()
    val uri = imageUriState.value

    if (name.isEmpty()) {
      nameErrorState.value = String.format(
        getString(CommonR.string.format_text_empty_error),
        getString(CommonR.string.input_hint_name),
      )
      isOK = false
    } else {
      nameErrorState.value = null
    }

    if (desc.length > 124) {
      descErrorState.value = "Maximum Characters"
      isOK = false
    } else {
      descErrorState.value = null
    }

    if (ModerationUtils.containsExplicitContent(name, desc)) {
      sharedViewModel.displayMsg(getString(CommonR.string.msg_explicit_content_error))
      isOK = false
    }

    if (uri == null) {
      sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_not_imported))
      isOK = false
    } else if (!isPngFormat(uri)) {
      sharedViewModel.displayMsg(getString(CommonR.string.msg_only_png_supported))
      isOK = false
    } else {
      val bitmap = requireContext().contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
      }
      if (bitmap == null || !hasAlphaChannel(bitmap)) {
        sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_no_alpha_error))
        isOK = false
      }
    }

    return isOK
  }

  companion object {
    private const val TAG = "NewPointerPost"
    private const val POINTER_SIZE = 128
  }
}

@Composable
fun NewPointerPostScreen(
  name: String,
  description: String,
  imageUri: Uri?,
  nameError: String?,
  descError: String?,
  adUnitId: String,
  onNameChange: (String) -> Unit,
  onDescChange: (String) -> Unit,
  onBrowseClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (adUnitId.isNotEmpty()) {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        AndroidView(
          factory = { context ->
            AdView(context).apply {
              setAdSize(AdSize.BANNER)
              this.adUnitId = adUnitId
              loadAd(AdRequest.Builder().build())
            }
          },
        )
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(CommonR.string.text_browse_pointer_image),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
        )

        Box(
          modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          val imageBitmap = ImageBitmap.imageResource(id = CommonR.drawable.transparent_rect)

          // Remember the brush so it doesn't get re-allocated on every recomposition
          val tiledBrush = remember(imageBitmap) {
            ShaderBrush(
              ImageShader(
                image = imageBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
              ),
            )
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(tiledBrush),
          )

          if (imageUri != null) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
              model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .build(),
            )
            Image(
              painter = painter,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
            )
          }
        }

        Button(
          onClick = onBrowseClick,
        ) {
          Text(text = stringResource(CommonR.string.text_action_browse))
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            painter = painterResource(CommonR.drawable.ic_info_round),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp),
          )
          Text(
            text = stringResource(CommonR.string.text_pointer_upload_notes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    OutlinedTextField(
      value = name,
      onValueChange = onNameChange,
      label = { Text(stringResource(CommonR.string.input_hint_name)) },
      supportingText = {
        Text(
          text = nameError ?: stringResource(CommonR.string.helper_text_name),
        )
      },
      isError = nameError != null,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = description,
      onValueChange = onDescChange,
      label = { Text(stringResource(CommonR.string.input_hint_desc)) },
      supportingText = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = descError ?: stringResource(CommonR.string.helper_text_desc),
            modifier = Modifier.weight(1f, fill = false),
          )
          Text(
            text = "${description.length} / 124",
          )
        }
      },
      isError = descError != null,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
