/*
 * Copyright (C) 2016 Sandip Vaghela (AfterROOT)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import static afterroot.pointerreplacer.Utils.getMimeType;
import static afterroot.pointerreplacer.Utils.showSnackbar;

public class UpdateActivity extends AppCompatActivity {
    private TextView textCurrentVersion;
    private TextView textNewVersion;
    private TextView textNewChangelog;
    private Button buttonUpdate;
    private CardView cardView_update;
    private LinearLayout layoutNoConnection;


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
        findViews();
    }

    private void findViews(){
        cardView_update = (CardView) findViewById(R.id.cardView_update);
        textCurrentVersion = (TextView) findViewById(R.id.textCurrentVersion);
        textNewVersion = (TextView) findViewById(R.id.textNewVersion);
        textNewChangelog = (TextView) findViewById(R.id.textNewChangelog);

        buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        layoutNoConnection = (LinearLayout) findViewById(R.id.layoutNoConnection);

        setup();
    }

    private void setup(){
        layoutNoConnection.setVisibility(View.GONE);
        cardView_update.setVisibility(View.GONE);
        if (!isNetworkAvailable()){
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
        if (isNetworkAvailable()){
            try {
                textCurrentVersion.setText(String.format("v%s (%s)",
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));

                String latestVersionCode = dlString(UpdateURLs.URL_VERSION_CODE, false);
                int vercode = Integer.valueOf(latestVersionCode);

                final String newVersionName = dlString(UpdateURLs.URL_VERSION_NAME, false);
                textNewVersion.setText(String.format("v%s (%s)", newVersionName, vercode));

                if (vercode > getPackageManager().getPackageInfo(getPackageName(), 0).versionCode){
                    buttonUpdate.setVisibility(View.VISIBLE);
                } else {
                    buttonUpdate.setEnabled(false);
                    buttonUpdate.setText(R.string.text_already_latest);
                }

                String changelog = dlString(UpdateURLs.URL_CHANGELOG, true);
                textNewChangelog.setText(changelog);

                final String apkURL = dlString(UpdateURLs.URL_DOWNLOAD_APK, false);

                buttonUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String apkName = "PR_"+newVersionName+".apk";
                        final File downlaodedApk = new File(Environment.getExternalStorageDirectory()+"/Pointer Replacer/Downloads/"+apkName);
                        if (downlaodedApk.exists()){
                            showSnackbar(findViewById(R.id.main_layoutUpdate), apkName+" already exists.");
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
                                    openFile(apkName, Uri.fromFile(downlaodedApk));
                                }
                            };
                            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                            showSnackbar(findViewById(R.id.main_layoutUpdate), "Downloading PR_"+ newVersionName+".apk");
                        }
                    }
                });
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            showSnackbar(findViewById(R.id.main_layoutUpdate), "No Connection");
        }
    }

    @NonNull
    private static String convertStreamToString(InputStream inputStream) throws UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null){
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void openFile(String filname, Uri downloadedFile){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(downloadedFile, getMimeType(filname));
        startActivity(intent);
    }

    private static String dlString(String url, boolean fetchLines) {
        String dS = "";
        try {
            DownloadTxtTask.setFetchLines(fetchLines);
            DownloadTxtTask DownloadTxtTask = new DownloadTxtTask ();
            dS = DownloadTxtTask.execute(new String[] {url}).get();
        } catch(Exception ex) {
            //
        }
        return dS;
    }

    public void retry(View view) {
        setup();
    }

    private static class DownloadTxtTask extends AsyncTask< String, Integer, String > {
        static boolean isFetchLines;

        static void setFetchLines(boolean fetchLines){
            isFetchLines = fetchLines;
        }

        @Override
        protected String doInBackground(String... downloadURL) {
            URL url;
            String result  = null;
            InputStream is;
            try {
                url = new URL(downloadURL[0]);
                is = url.openStream();
                if (isFetchLines){
                    result = convertStreamToString(is);
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    result = br.readLine();
                    br.close();
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

        }
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
