/*
 * Copyright (C) 2011-2013, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 */

package sheetrock.panda.changelog

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Color
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.WebView
import com.afterroot.pointerdash.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ChangeLog
/**
 * Constructor
 *
 *
 * Retrieves the version names and stores the new version name in SharedPreferences
 *
 * @param context
 * @param sp      the shared preferences to store the last version name into
 */
private constructor(private val context: Context, sp: SharedPreferences) {
    /**
     * @return The version name of the last installation of this app (as described in the former
     * manifest). This will be the same as returned by `getThisVersion()` the
     * second time this version of the app is launched (more precisely: the second time
     * ChangeLog is instantiated).
     * //@see AndroidManifest.xml#android:versionName
     */
    private var lastVersion: String? = null
    /**
     * @return The version name of this app as described in the manifest.
     * //@see AndroidManifest.xml#android:versionName
     */
    private var thisVersion: String? = null
    private var listMode = Listmode.NONE
    private var sb: StringBuffer? = null

    /**
     * @return An AlertDialog displaying the changes since the previous installed version of your
     * app (what's new). But when this is the first run of your app including ChangeLog then
     * the full log dialog is show.
     */
    val logDialog: AlertDialog
        get() = this.getDialog(this.firstRunEver())

    /**
     * @return an AlertDialog with a full change log displayed
     */
    val fullLogDialog: AlertDialog
        get() = this.getDialog(true)

    /**
     * @return HTML displaying the changes since the previous installed version of your app (what's
     * new)
     */
    val log: String
        get() = this.getLog(false)

    /**
     * @return HTML which displays full change log
     */
    val fullLog: String
        get() = this.getLog(true)

    /**
     * Constructor
     *
     *
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param context
     */
    constructor(context: Context) : this(context, PreferenceManager.getDefaultSharedPreferences(context))

    init {

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION)
        //Log.d(TAG, "lastVersion: " + lastVersion);
        try {
            this.thisVersion = context.packageManager.getPackageInfo(context.packageName,
                    0).versionName
        } catch (e: NameNotFoundException) {
            this.thisVersion = NO_VERSION
            Log.e(TAG, "could not get version name from manifest!")
            e.printStackTrace()
        }

        //Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /**
     * @return `true` if this version of your app is started the first time
     */
    fun firstRun(): Boolean {
        return this.lastVersion != this.thisVersion
    }

    /**
     * @return `true` if your app including ChangeLog is started the first time ever.
     * Also `true` if your app was uninstalled and installed again.
     */
    private fun firstRunEver(): Boolean {
        return NO_VERSION == this.lastVersion
    }

    private fun getDialog(full: Boolean): AlertDialog {
        val wv = WebView(this.context)

        //wv.setBackgroundColor(Color.parseColor(context.getResources().getString(
        //R.string.background_color)));
        wv.setBackgroundColor(Color.DKGRAY)
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html", "UTF-8", null)

        val builder = AlertDialog.Builder(ContextThemeWrapper(this.context,
                R.style.Theme_AppCompat_Dialog))
        builder.setTitle(
                context.resources.getString(
                        if (full) R.string.changelog_full_title else R.string.changelog_title))
                .setView(wv)
                .setCancelable(false)
                // OK button
                .setPositiveButton(context.resources.getString(R.string.changelog_ok_button)
                ) { dialog, which -> updateVersionInPreferences() }

        if (!full) {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full
            ) { dialog, id -> fullLogDialog.show() }
        }

        return builder.create()
    }

    private fun updateVersionInPreferences() {
        // save new version number to preferences
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sp.edit()
        editor.putString(VERSION_KEY, thisVersion)
        // // on SDK-Versions > 9 you should use this:
        // if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
        // editor.commit();
        // } else {
        // editor.apply();
        // }
        editor.apply()
    }

    private fun getLog(full: Boolean): String {
        // read changelog.txt file
        sb = StringBuffer()
        try {
            val ins = context.resources.openRawResource(R.raw.changelog)
            val br = BufferedReader(InputStreamReader(ins))

            var line: String? = null
            var advanceToEOVS = false // if true: ignore further version
            // sections
            while ((br.readLine()) != null) {
                line = line!!.trim { it <= ' ' }
                val marker = if (line.isNotEmpty()) line[0] else 0.toChar()
                if (marker == '$') {
                    // begin of a version section
                    this.closeList()
                    val version = line.substring(1).trim { it <= ' ' }
                    // stop output?
                    if (!full) {
                        if (this.lastVersion == version) {
                            advanceToEOVS = true
                        } else if (version == EOCL) {
                            advanceToEOVS = false
                        }
                    }
                } else if (!advanceToEOVS) {
                    when (marker) {
                        '%' -> {
                            // line contains version title
                            this.closeList()
                            sb!!.append("<div class='title'>").append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }
                        '_' -> {
                            // line contains version title
                            this.closeList()
                            sb!!.append("<div class='subtitle'>").append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }
                        '!' -> {
                            // line contains free text
                            this.closeList()
                            sb!!.append("<div class='freetext'>").append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }
                        '#' -> {
                            // line contains numbered list item
                            this.openList(Listmode.ORDERED)
                            sb!!.append("<li>").append(line.substring(1).trim { it <= ' ' }).append("</li>\n")
                        }
                        '*' -> {
                            // line contains bullet list item
                            this.openList(Listmode.UNORDERED)
                            sb!!.append("<li>").append(line.substring(1).trim { it <= ' ' }).append("</li>\n")
                        }
                        else -> {
                            // no special character: just use line as is
                            this.closeList()
                            sb!!.append(line).append("\n")
                        }
                    }
                }
            }
            this.closeList()
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return sb!!.toString()
    }

    private fun openList(listMode: Listmode) {
        if (this.listMode != listMode) {
            closeList()
            if (listMode == Listmode.ORDERED) {
                sb!!.append("<div class='list'><ol>\n")
            } else if (listMode == Listmode.UNORDERED) {
                sb!!.append("<div class='list'><ul>\n")
            }
            this.listMode = listMode
        }
    }

    private fun closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb!!.append("</ol></div>\n")
        } else if (this.listMode == Listmode.UNORDERED) {
            sb!!.append("</ul></div>\n")
        }
        this.listMode = Listmode.NONE
    }

    /**
     * manually set the last version name - for testing purposes only
     *
     * @param lastVersion
     */
    fun dontuseSetLastVersion(lastVersion: String) {
        this.lastVersion = lastVersion
    }

    /**
     * modes for HTML-Lists (bullet, numbered)
     */
    private enum class Listmode {
        NONE, ORDERED, UNORDERED
    }

    companion object {

        // this is the key for storing the version name in SharedPreferences
        private val VERSION_KEY = "PREFS_VERSION_KEY"
        private val NO_VERSION = ""
        private val EOCL = "END_OF_CHANGE_LOG"
        private val TAG = "Pointer Replacer"
    }
}
