package app.tivi.util

import android.util.Log
import app.tivi.app.ApplicationInfo
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import timber.log.Timber

class TimberLogger @Inject constructor(
  applicationInfo: ApplicationInfo,
  private val firebaseCrashlytics: FirebaseCrashlytics,
) : Logger {

  init {
    if (applicationInfo.debugBuild) {
      Timber.plant(ERPDebugTree())
    }

    try {
      Timber.plant(CrashlyticsTree(firebaseCrashlytics))
    } catch (e: IllegalStateException) {
      // Firebase is likely not setup in this project. Ignore the exception
    }
  }

  override fun setUserId(id: String) {
    try {
      firebaseCrashlytics.setCustomKey("username", id)
    } catch (t: Throwable) {
      // Firebase might not be setup
    }
  }

  override fun v(throwable: Throwable?, message: () -> String) {
    Timber.v(throwable, message())
  }

  override fun d(throwable: Throwable?, message: () -> String) {
    Timber.d(throwable, message())
  }

  override fun i(throwable: Throwable?, message: () -> String) {
    Timber.i(throwable, message())
  }

  override fun e(throwable: Throwable?, message: () -> String) {
    Timber.e(throwable, message())
  }

  override fun w(throwable: Throwable?, message: () -> String) {
    Timber.w(throwable, message())
  }
}

private class ERPDebugTree : Timber.DebugTree() {
  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    super.log(
      priority,
      createStackElementTag(Throwable().stackTrace[7]) ?: "ERP",
      message,
      t,
    )
  }
}

class CrashlyticsTree @Inject constructor(private val firebaseCrashlytics: FirebaseCrashlytics) :
  Timber.Tree() {
  override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    firebaseCrashlytics.log(message)
    if (t != null) {
      firebaseCrashlytics.recordException(t)
    }
  }
}
