// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class AndroidSetCrashReportingEnabledAction @Inject constructor(
  private val firebaseCrashlytics: FirebaseCrashlytics,
) : SetCrashReportingEnabledAction {
  override fun invoke(enabled: Boolean) {
    firebaseCrashlytics.isCrashlyticsCollectionEnabled = enabled
  }
}
