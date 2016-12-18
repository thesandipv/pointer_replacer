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

package afterroot.pointerreplacer;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;


public class UpdateActivity extends AppCompatActivity {
    private TextView textCurrentVersion;
    private TextView textNewVersion;
    private TextView textNewChangelog;
    private TextView textUpdateNow;
    private CardView cardView_update;
    private LinearLayout layoutNoConnection;
    private Utils mUtils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }


        initialize();
    }

    @Override
    protected void onResume() {
        setup();
        super.onResume();
    }

    private void initialize(){
        mUtils = new Utils();

        findViews();
    }

    private void findViews(){
        cardView_update = (CardView) findViewById(R.id.cardView_update);
        textCurrentVersion = (TextView) findViewById(R.id.textCurrentVersion);
        textNewVersion = (TextView) findViewById(R.id.textNewVersion);
        textNewChangelog = (TextView) findViewById(R.id.textNewChangelog);
        textUpdateNow = (TextView) findViewById(R.id.text_update_now);
        layoutNoConnection = (LinearLayout) findViewById(R.id.layoutNoConnection);

        setup();
    }

    private void setup(){
        layoutNoConnection.setVisibility(View.GONE);
        cardView_update.setVisibility(View.GONE);
        if (!mUtils.isNetworkAvailable(this)){
            layoutNoConnection.setVisibility(View.VISIBLE);
        } else {
            checkForUpdate();
        }
    }

    private void checkForUpdate() {
        setupUpdater();
        cardView_update.setVisibility(View.VISIBLE);
    }

    private void setupUpdater(){
        if (mUtils.isNetworkAvailable(this)){
            try {
                textCurrentVersion.setText(String.format("v%s (%s)",
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));

                String latestVersionCode = mUtils.dlString(UpdateURLs.URL_VERSION_CODE, false);
                int vercode = Integer.valueOf(latestVersionCode);

                final String newVersionName = mUtils.dlString(UpdateURLs.URL_VERSION_NAME, false);
                textNewVersion.setText(String.format("v%s (%s)", newVersionName, vercode));

                if (vercode > getPackageManager().getPackageInfo(getPackageName(), 0).versionCode){
                    textUpdateNow.setVisibility(View.VISIBLE);
                } else {
                    textUpdateNow.setText(R.string.text_already_latest);
                }

                String changelog = mUtils.dlString(UpdateURLs.URL_CHANGELOG, true);
                textNewChangelog.setText(changelog);

                final String apkURL = mUtils.dlString(UpdateURLs.URL_DOWNLOAD_APK, false);

                textUpdateNow.setOnClickListener(view -> {
                    final String apkName = "PR_"+newVersionName+".apk";
                    final File downlaodedApk = new File(Environment.getExternalStorageDirectory()+"/Pointer Replacer/Downloads/"+apkName);
                    if (downlaodedApk.exists()){
                        mUtils.showSnackbar(findViewById(R.id.main_layoutUpdate), apkName+" already exists.");
                    } else {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkURL));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setVisibleInDownloadsUi(true);
                        request.setDestinationInExternalPublicDir("/Pointer Replacer/Downloads/", apkName);
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        BroadcastReceiver onComplete = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                mUtils.openFile(UpdateActivity.this, apkName, Uri.fromFile(downlaodedApk));
                            }
                        };
                        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                        mUtils.showSnackbar(findViewById(R.id.main_layoutUpdate), "Downloading PR_"+ newVersionName+".apk");
                    }
                });
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            mUtils.showSnackbar(findViewById(R.id.main_layoutUpdate), "No Connection");
        }
    }

    public void retry(View view) {
        setup();
    }

    private class UpdateURLs {

        static final String
                URL_GITHUB = "https://raw.githubusercontent.com/",
                URL_REPO = URL_GITHUB + "sandipv22/pointer_replacer/main/version_checker/",
                URL_VERSION_CODE = URL_REPO + "version_code.txt",
                URL_VERSION_NAME = URL_REPO + "version_name.txt",
                URL_CHANGELOG = URL_REPO + "changelog.txt",
                URL_DOWNLOAD_APK = URL_REPO + "download_url.txt";

    }
}
