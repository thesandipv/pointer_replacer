package com.afterroot.allusive2.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tivi.util.Logger
import com.afterroot.allusive2.data.model.OssLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

@HiltViewModel
class AcknowledgementsViewModel @Inject constructor(
  application: Application,
  json: Json,
  logger: Logger,
) : AndroidViewModel(application) {

  val ossLibraries: StateFlow<List<OssLibrary>> = flow {
    val jsonString = application.resources.assets.open(ACKNOWLEDGEMENTS_FILE_PATH)
      .bufferedReader().use { it.readText() }
    val libraries = json.decodeFromString<List<OssLibrary>>(jsonString)
      .asSequence()
      .distinctBy { "${it.groupId}:${it.artifactId}" }
      .sortedBy { it.name }
      .toList()
    emit(libraries)
  }.catch { e ->
    logger.e(e) { e.message ?: "Error while loading OSS libraries" }
    emit(emptyList())
  }.flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Lazily,
      initialValue = emptyList(),
    )

  companion object {
    private const val ACKNOWLEDGEMENTS_FILE_PATH = "licences/licenses.json"
  }
}
