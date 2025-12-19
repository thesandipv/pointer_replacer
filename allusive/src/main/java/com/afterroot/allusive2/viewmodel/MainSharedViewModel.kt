/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.data.FirestorePagingSource
import com.afterroot.allusive2.home.HomeActions
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainSharedViewModel @Inject constructor(
  val savedStateHandle: SavedStateHandle,
  private val remoteConfig: FirebaseRemoteConfig,
  private val firebaseFirestore: FirebaseFirestore,
  private val firebaseUtils: FirebaseUtils,
  private val settings: Settings,
) : ViewModel() {
  private val _snackbarMsg = MutableLiveData<Event<String>>()
  val liveTitle = MutableLiveData<String>()

  var actions = MutableSharedFlow<HomeActions>()
    private set

  init {
    Timber.d("init $this")

    remoteConfig.fetchAndActivate().addOnSuccessListener {
      savedStateHandle[KEY_CONFIG_LOADED] = true
    }

    viewModelScope.launch {
      actions.collect { action ->
        Timber.d("action: $action")
        when (action) {
          // Add actions should be handled by ViewModel.
          else -> {}
        }
      }
    }
  }

  val pointers = Pager(PagingConfig(20)) {
    FirestorePagingSource(firebaseFirestore, settings, firebaseUtils)
  }.flow.cachedIn(viewModelScope)

  internal fun submitAction(action: HomeActions) {
    viewModelScope.launch {
      actions.emit(action)
    }
  }

  fun setTitle(title: String?) {
    if (liveTitle.value != title) { // Don't change title if new title is equal to old.
      liveTitle.value = title!!
    }
  }

  val snackbarMsg: LiveData<Event<String>>
    get() = _snackbarMsg

  fun displayMsg(msg: String) {
    _snackbarMsg.value = Event(msg)
  }

  fun loadIntAdInterstitial(isShow: Boolean = false) {
    submitAction(HomeActions.LoadIntAd(isShow))
  }

  fun showInterstitialAd() {
    submitAction(HomeActions.ShowIntAd)
  }

  companion object {
    const val KEY_CONFIG_LOADED = "configLoaded"
  }
}
