/*
 * Copyright (C) 2016 Sandip Vaghela
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

package com.afterroot.allusive;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ChangeLog {

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";
    private static final String NO_VERSION = "";
    private static final String EOCL = "END_OF_CHANGE_LOG";
    private static final String TAG = "Pointer Replacer";
    private final Context context;
    private String lastVersion, thisVersion;
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    /**
     * Constructor
     *
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param context
     */
    ChangeLog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Constructor
     *
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param context
     * @param sp
     *            the shared preferences to store the last version name into
     */
    private ChangeLog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        //Log.d(TAG, "lastVersion: " + lastVersion);
        try {
            this.thisVersion = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0).versionName;
        } catch (NameNotFoundException e) {
            this.thisVersion = NO_VERSION;
            Log.e(TAG, "could not get version name from manifest!");
            e.printStackTrace();
        }
        //Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /**
     * @return The version name of the last installation of this app (as described in the former
     *         manifest). This will be the same as returned by <code>getThisVersion()</code> the
     *         second time this version of the app is launched (more precisely: the second time
     *         ChangeLog is instantiated).
     * //@see AndroidManifest.xml#android:versionName
     */
    public String getLastVersion() {
        return this.lastVersion;
    }

    /**
     * @return The version name of this app as described in the manifest.
     * //@see AndroidManifest.xml#android:versionName
     */
    public String getThisVersion() {
        return this.thisVersion;
    }

    /**
     * @return <code>true</code> if this version of your app is started the first time
     */
    boolean firstRun() {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return <code>true</code> if your app including ChangeLog is started the first time ever.
     *         Also <code>true</code> if your app was deinstalled and installed again.
     */
    private boolean firstRunEver() {
        return NO_VERSION.equals(this.lastVersion);
    }

    /**
     * @return An AlertDialog displaying the changes since the previous installed version of your
     *         app (what's new). But when this is the first run of your app including ChangeLog then
     *         the full log dialog is show.
     */
    AlertDialog getLogDialog() {
        return this.getDialog(this.firstRunEver());
    }

    /**
     * @return an AlertDialog with a full change log displayed
     */
    AlertDialog getFullLogDialog() {
        return this.getDialog(true);
    }

        private AlertDialog getDialog(boolean full) {
        WebView wv = new WebView(this.context);

        //wv.setBackgroundColor(Color.parseColor(context.getResources().getString(
                //R.string.background_color)));
        wv.setBackgroundColor(Color.DKGRAY);
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this.context,
                R.style.Theme_AppCompat_Dialog));
        builder.setTitle(
                context.getResources().getString(
                        full ? R.string.changelog_full_title : R.string.changelog_title))
                .setView(wv)
                .setCancelable(false)
                // OK button
                .setPositiveButton(context.getResources().getString(R.string.changelog_ok_button),
                        (dialog, which) -> updateVersionInPreferences());

        if (!full) {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full,
                    (dialog, id) -> getFullLogDialog().show());
        }

        return builder.create();
    }

    private void updateVersionInPreferences() {
        // save new version number to preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, thisVersion);
        // // on SDK-Versions > 9 you should use this:
        // if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
        // editor.commit();
        // } else {
        // editor.apply();
        // }
        editor.apply();
    }

    /**
     * @return HTML displaying the changes since the previous installed version of your app (what's
     *         new)
     */
    public String getLog() {
        return this.getLog(false);
    }

    /**
     * @return HTML which displays full change log
     */
    public String getFullLog() {
        return this.getLog(true);
    }

    protected String getLog(boolean full) {
        // read changelog.txt file
        sb = new StringBuffer();
        try {
            InputStream ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));

            String line = null;
            boolean advanceToEOVS = false; // if true: ignore further version
                                           // sections
            while ((line = br.readLine()) != null) {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$') {
                    // begin of a version section
                    this.closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full) {
                        if (this.lastVersion.equals(version)) {
                            advanceToEOVS = true;
                        } else if (version.equals(EOCL)) {
                            advanceToEOVS = false;
                        }
                    }
                } else if (!advanceToEOVS) {
                    switch (marker) {
                    case '%':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='title'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '_':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='subtitle'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '!':
                        // line contains free text
                        this.closeList();
                        sb.append("<div class='freetext'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '#':
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                        break;
                    case '*':
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
                        sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                        break;
                    default:
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line + "\n");
                    }
                }
            }
            this.closeList();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    protected void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == Listmode.ORDERED) {
                sb.append("<div class='list'><ol>\n");
            } else if (listMode == Listmode.UNORDERED) {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    protected void closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb.append("</ol></div>\n");
        } else if (this.listMode == Listmode.UNORDERED) {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }

    /**
     * manually set the last version name - for testing purposes only
     *
     * @param lastVersion
     */
    public void dontuseSetLastVersion(String lastVersion) {
        this.lastVersion = lastVersion;
    }

/** modes for HTML-Lists (bullet, numbered) */
    private enum Listmode {
        NONE, ORDERED, UNORDERED,
    }
}
