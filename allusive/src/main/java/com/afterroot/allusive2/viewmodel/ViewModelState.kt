/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.Observer

sealed class ViewModelState {
  object Loading : ViewModelState()
  data class Loaded<T>(val data: T) : ViewModelState()
}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T) {

  var hasBeenHandled = false
    private set // Allow external read but not write

  /**
   * Returns the content and prevents its use again.
   */
  fun getContentIfNotHandled(): T? = if (hasBeenHandled) {
    null
  } else {
    hasBeenHandled = true
    content
  }

  /**
   * Returns the content, even if it's already been handled.
   */
  fun peekContent(): T = content
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
  override fun onChanged(event: Event<T>) {
    event.getContentIfNotHandled()?.let { value ->
      onEventUnhandledContent(value)
    }
  }
}
