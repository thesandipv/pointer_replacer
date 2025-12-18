/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuProvider
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.data.mapper.toPointer
import com.afterroot.allusive2.data.mapper.toRoomPointer
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.data.requests
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.databinding.DialogEditPointerBinding
import com.afterroot.allusive2.databinding.FragmentPointerInfoBinding
import com.afterroot.allusive2.databinding.FragmentPointerRepoBinding
import com.afterroot.allusive2.getPointerSaveDir
import com.afterroot.allusive2.home.HomeActions
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.model.PointerRequest
import com.afterroot.allusive2.repo.PointerPagingAdapter
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.showStaticProgressDialog
import com.afterroot.utils.extensions.visible
import com.afterroot.utils.getMaterialColor
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.jetbrains.anko.doFromSdk
import org.jetbrains.anko.toast
import timber.log.Timber
import com.afterroot.allusive2.repo.R as RepoR
import com.afterroot.allusive2.resources.R as CommonR
import com.google.android.material.R as MaterialR

@AndroidEntryPoint
class PointersRepoFragment :
  Fragment(),
  ItemSelectedCallback<Pointer> {

  @Inject lateinit var firebaseUtils: FirebaseUtils

  @Inject lateinit var firestore: FirebaseFirestore

  @Inject lateinit var myDatabase: MyDatabase

  @Inject lateinit var settings: Settings

  @Inject lateinit var storage: FirebaseStorage
  private lateinit var binding: FragmentPointerRepoBinding
  private lateinit var fabApply: ExtendedFloatingActionButton
  private lateinit var pointersPagingAdapter: PointerPagingAdapter
  private lateinit var targetPath: String
  private val networkViewModel: NetworkViewModel by activityViewModels()
  private val sharedViewModel: MainSharedViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentPointerRepoBinding.inflate(inflater, container, false)
    fabApply = requireActivity().findViewById(R.id.fab_apply)
    return binding.root
  }

  override fun onResume() {
    super.onResume()
    networkViewModel.monitor(
      this,
      onConnect = {
        onNetworkChange(true)
      },
      onDisconnect = {
        onNetworkChange(false)
      },
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (firebaseUtils.isUserSignedIn) {
      targetPath = requireContext().getPointerSaveDir()
      // setUpList()
      setUpAdapter()
      loadPointers()

      requireActivity().addMenuProvider(
        object : MenuProvider {
          override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(RepoR.menu.menu_repo, menu)
          }

          override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            android.R.id.home -> {
              false
            }

            RepoR.id.repo_request_status -> {
              findNavController().navigate(R.id.repo_to_rro_request)
              true
            }

            else -> menuItem.onNavDestinationSelected(findNavController())
          }
        },
        viewLifecycleOwner,
      )
    }
  }

  private fun onNetworkChange(isAvailable: Boolean) {
    binding.repoSwipeRefresh.visible(isAvailable)
    binding.layoutNoNetwork.visible(!isAvailable)
    if (!isAvailable) {
      binding.buttonRetry.setOnClickListener {
        onResume()
      }
      fabApply.hide()
    } else {
      fabApply.apply {
        show()
        setOnClickListener {
          requireActivity().findNavController(
            R.id.fragment_repo_nav,
          ).navigate(R.id.repo_to_new_pointer)
        }
        icon = requireContext().getDrawableExt(CommonR.drawable.ic_add)
      }

      binding.repoSwipeRefresh.apply {
        setOnRefreshListener {
          try {
            // loadPointers()
            refreshData()
          } catch (e: IllegalStateException) {
            isRefreshing = false
            e.printStackTrace()
          }
        }

        setBackgroundColor(getMaterialColor(MaterialR.attr.colorSurfaceVariant))
        setColorSchemeColors(
          getMaterialColor(MaterialR.attr.colorPrimary),
          getMaterialColor(MaterialR.attr.colorSecondary),
        )
      }
    }
  }

  // Function for using new list adapter.
  private fun setUpAdapter() {
    pointersPagingAdapter = PointerPagingAdapter(this, storage)
    binding.list.apply {
      val lm = LinearLayoutManager(requireContext())
      layoutManager = lm
      addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
      adapter = pointersPagingAdapter
      setHasFixedSize(true)
      doFromSdk(Build.VERSION_CODES.LOLLIPOP) { FastScrollerBuilder(this).build() }
    }
    setUpFilter()
  }

  private fun displayPointers(pagedData: PagingData<Pointer>) {
    lifecycleScope.launch {
      pointersPagingAdapter.submitData(pagedData)
    }
  }

  private fun loadPointers() {
    lifecycleScope.launch {
      sharedViewModel.pointers.collectLatest {
        displayPointers(pagedData = it)
      }
    }

    lifecycleScope.launch {
      pointersPagingAdapter.loadStateFlow.collectLatest {
        binding.repoSwipeRefresh.isRefreshing =
          it.refresh is LoadState.Loading ||
          it.append is LoadState.Loading
      }
    }
  }

  private fun refreshData() {
    pointersPagingAdapter.refresh()
  }

  private fun setUpFilter() {
    binding.repoSortChipGroup.apply {
      when (settings.orderBy) {
        DatabaseFields.FIELD_TIME -> this.check(R.id.filter_chip_sort_by_date)
        DatabaseFields.FIELD_DOWNLOADS -> this.check(R.id.filter_chip_sort_by_download)
      }
    }
    binding.repoFilterChipGroup.apply {
      if (settings.filterRRO) {
        this.check(R.id.filter_chip_show_only_rro)
      }
    }

    binding.filterChipSortByDate.apply {
      isChipIconVisible = !isChecked
      setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          settings.orderBy = DatabaseFields.FIELD_TIME
          refreshData()
        }
        isChipIconVisible = !isChecked
      }
    }
    binding.filterChipSortByDownload.apply {
      isChipIconVisible = !isChecked
      setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          settings.orderBy = DatabaseFields.FIELD_DOWNLOADS
          refreshData()
        }
        isChipIconVisible = !isChecked
      }
    }

    binding.filterChipShowUserUploaded.apply {
      visible(false)
      isChipIconVisible = !isChecked
      setOnCheckedChangeListener { _, isChecked ->
        isCloseIconVisible = isChecked
        isChipIconVisible = !isChecked

        // TODO
        lifecycleScope.launch {
          settings.filterUserPointers = isChecked
          delay(200)
          refreshData()
        }
      }
      setOnCloseIconClickListener {
        isChecked = false
      }
    }

    binding.filterChipShowOnlyRro.apply {
      isChipIconVisible = !isChecked
      setOnCheckedChangeListener { _, isChecked ->
        isCloseIconVisible = isChecked
        isChipIconVisible = !isChecked
        lifecycleScope.launch {
          settings.filterRRO = isChecked
          delay(200)
          refreshData()
        }
      }
      setOnCloseIconClickListener {
        isChecked = false
      }
    }
  }

  private fun showPointerInfoDialog(pointer: Pointer) {
    // FIXME check this is working or not
    val binding = FragmentPointerInfoBinding.inflate(layoutInflater)
    val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
      customView(view = binding.root, scrollable = true)
    }

    dialog.getCustomView().apply {
      when (pointer.reasonCode) {
        Reason.OK -> {
          binding.pointer = pointer
          binding.infoPointerImage.apply {
            updateLayoutParams<ConstraintLayout.LayoutParams> {
              height = 128
              width = 128
            }
            val storageReference = storage.reference
              .child("${DatabaseFields.COLLECTION_POINTERS}/${pointer.filename}")
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(
              true,
            ).build()
            background = context.getDrawableExt(CommonR.drawable.transparent_grid)
            Glide.with(context)
              .load(storageReference)
              .override(128, 128)
              .transition(DrawableTransitionOptions.withCrossFade(factory))
              .into(this)
          }
          binding.infoActionPack.apply {
            lifecycleScope.launch {
              if (myDatabase.pointerDao().exists(pointer.filename!!).isNotEmpty()) {
                text = getString(CommonR.string.text_installed)
                setOnClickListener(null)
                isEnabled = false
              } else {
                text = getString(CommonR.string.text_download)
                setOnClickListener {
                  downloadPointer(pointer)
                  dialog.dismiss()
                }
                isEnabled = pointer.reasonCode == Reason.OK
              }
            }
          }
          binding.infoActionInstallRro.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
              visible(false)
            } else {
              visible(true)
              if (pointer.hasRRO) {
                text = getString(CommonR.string.text_install_rro)
                setOnClickListener {
                  showInterstitialAd {
                    if (this@PointersRepoFragment.findNavController().currentDestination?.id ==
                      R.id.repoFragment
                    ) {
                      val directions = PointersRepoFragmentDirections.repoToRroInstall(
                        pointer.docId.toString(),
                        pointer.filename.toString(),
                      )
                      this@PointersRepoFragment.findNavController().navigate(
                        directions,
                      )
                    }
                    dialog.dismiss()
                  }
                }
              } else {
                text = getString(CommonR.string.text_request_rro)
                firestore.pointers().document(
                  pointer.docId.toString(),
                ).get(Source.CACHE)
                  .addOnSuccessListener { localDoc ->
                    val localPointer = localDoc.toPointer()
                    isEnabled = localPointer?.rroRequested == false
                  }.addOnFailureListener {
                    Timber.e(it, "showPointerInfoDialog: ${it.message}")
                    firestore.pointers().document(
                      pointer.docId.toString(),
                    ).get()
                      .addOnSuccessListener { serverDoc ->
                        val localPointer = serverDoc.toPointer()
                        isEnabled = localPointer?.rroRequested == false
                      }
                  }
                setOnClickListener {
                  showInterstitialAd {
                    lifecycleScope.launch {
                      firestore.pointers().document(pointer.docId.toString())
                        .update(DatabaseFields.FIELD_RRO_REQUESTED, true)
                        .await()
                      val request = PointerRequest(
                        pointer.filename,
                        firebaseUtils.uid,
                        documentId = pointer.docId,
                      )
                      firestore.requests().document(
                        pointer.docId.toString(),
                      ).set(request)
                        .addOnSuccessListener {
                          isEnabled = false
                          requireContext().toast("RRO Requested")
                        }.addOnFailureListener {
                          Timber.d("showPointerInfoDialog: ${it.message}")
                          it.printStackTrace()
                        }
                    }
                  }
                }
              }
            }
          }
          val downloads = resources.getQuantityString(
            CommonR.plurals.str_format_download_count,
            pointer.downloads,
            pointer.downloads,
          )
          val date = SimpleDateFormat.getDateInstance().format(pointer.time)
          binding.downloadsText =
            String.format(
              getString(CommonR.string.format_text_downloads_and_date),
              downloads,
              date,
            )
          pointer.uploadedBy?.forEach {
            binding.uploadedBy =
              String.format(
                context.getString(CommonR.string.str_format_uploaded_by),
                it.value,
              )
          }
          binding.infoPointerId.text =
            getString(
              CommonR.string.format_text_info_pointer_id,
              pointer.docId.toString(),
              pointer.filename?.substringBeforeLast("."),
            )
        }

        else -> {
          binding.infoPointerImage.apply {
            updateLayoutParams<ConstraintLayout.LayoutParams> {
              height = 128
              width = 128
            }
            background = null
            setImageDrawable(
              context.getDrawableExt(
                CommonR.drawable.ic_removed,
                getMaterialColor(MaterialR.attr.colorError),
              ),
            )
          }
          binding.apply {
            this.pointer = pointer
            downloadsText = null
            uploadedBy = null
            infoActionPack.visible(false)
          }
        }
      }
    }
  }

  private fun downloadPointer(pointer: Pointer) {
    val dialog = requireContext().showStaticProgressDialog(
      getString(CommonR.string.text_progress_downloading),
    )
    val ref = storage.pointers().child(pointer.filename!!)
    ref.getFile(File("$targetPath${pointer.filename}"))
      .addOnSuccessListener {
        sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_downloaded))
        lifecycleScope.launch {
          if (!BuildConfig.DEBUG) {
            pointer.docId?.let { it1 ->
              firestore.pointers().document(it1)
                .update(DatabaseFields.FIELD_DOWNLOADS, FieldValue.increment(1))
            }
          }

          myDatabase.pointerDao().add(pointer.toRoomPointer())
        }

        dialog.dismiss()
      }.addOnFailureListener {
        sharedViewModel.displayMsg(getString(CommonR.string.msg_error))
        dialog.dismiss()
      }
  }

  private fun showEditPointerDialog(pointer: Pointer) {
    val binding = DialogEditPointerBinding.inflate(layoutInflater)
    val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
    dialog.show {
      customView(view = binding.root)
      title(text = "Edit Pointer Info")
      val oldTitle = pointer.name
      val oldDesc = pointer.description
      binding.tiTitle.setText(oldTitle)
      binding.tiDesc.setText(oldDesc)
      positiveButton(res = CommonR.string.text_action_save) {
        val newTitle = binding.tiTitle.text.toString()
        val newDesc = binding.tiDesc.text.toString()
        if (oldTitle != newTitle || oldDesc != newDesc) {
          lifecycleScope.launch {
            val snapshot = firestore.pointers().whereEqualTo(
              DatabaseFields.FIELD_FILENAME,
              pointer.filename,
            )
              .get(Source.CACHE).await()
            val docId = snapshot.documents.first().id
            val updates = mapOf(
              Pair(DatabaseFields.FIELD_NAME, newTitle),
              Pair(DatabaseFields.FIELD_DESC, newDesc),
            )
            firestore.pointers().document(docId).update(updates).addOnCompleteListener {
              refreshData()
            }
          }
        } else {
          // No changes to save. dismiss dialog.
          dismiss()
        }
      }
    }
  }

  private fun showDeleteDialog(pointer: Pointer, position: Int) {
    MaterialDialog(requireContext()).show {
      message(text = getString(CommonR.string.dialog_delete_confirm))
      positiveButton(CommonR.string.text_delete) {
        lifecycleScope.launch {
          pointer.docId?.let {
            firestore.pointers().document(it).delete().await()
            binding.list.scrollToPosition(position)
            // delete pointer from storage bucket
            storage.reference.child(DatabaseFields.COLLECTION_POINTERS)
              .child(pointer.filename!!)
              .delete()
            context.toast(CommonR.string.msg_delete_success)
          }
        }
      }
      negativeButton(android.R.string.cancel) {
        it.dismiss()
      }
    }
  }

  override fun onClick(position: Int, view: View?, item: Pointer) {
    showPointerInfoDialog(item)
  }

  @SuppressLint("CheckResult")
  override fun onLongClick(position: Int, item: Pointer): Boolean {
    val result = kotlin.runCatching {
      if (!item.uploadedBy!!.containsKey(firebaseUtils.uid) || item.reasonCode != Reason.OK) {
        return false
      }
      val list =
        mutableListOf(
          getString(CommonR.string.text_edit),
          getString(CommonR.string.text_delete),
        )
      MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        cornerRadius(16f)
        listItems(items = list) { _, _, text ->
          when (text) {
            getString(CommonR.string.text_edit) -> {
              showEditPointerDialog(item)
            }

            getString(CommonR.string.text_delete) -> {
              showDeleteDialog(item, position)
            }
          }
        }
      }
    }
    result.onFailure {
      it.message?.let { it1 -> requireContext().toast(it1) }
    }
    return true
  }

  private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Timber.e(throwable, "CoroutineException: ${throwable.message}")
  }

  private fun showInterstitialAd(onAdDismiss: () -> Unit = {}) {
    sharedViewModel.showInterstitialAd()
    lifecycleScope.launch(coroutineExceptionHandler) {
      sharedViewModel.actions.collectLatest { action ->
        if (action is HomeActions.OnIntAdDismiss) {
          onAdDismiss()
        }
        coroutineContext.job.cancel()
      }
    }
  }
}
