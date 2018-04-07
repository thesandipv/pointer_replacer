/*
 * Copyright (C) 2016-2017 Sandip Vaghela
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.allusive.adapter.PointerAdapter;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ManagePointerActivity extends AppCompatActivity {

    private String mTargetPath;
    private String mTag;
    private Helper mUtils;
    private File[] targetFiles;
    private File target;
    private SharedPreferences mInstalledPointerPrefs;
    private SharedPreferences.Editor mInstalledPointerEditor;
    private PointerListAdapter pointerListAdapter;

    private static String DEFAULT_POINTER_PACK = "pointer";
    private static String CHRISTMAS_POINTER_PACK = "Christmas";
    private static String HEART_POINTER_PACK = "emoji";
    private static String GOOGLE_MI_PACK = "GoogleMaterialIcons";
    private static String HERMANKZR_PACK = "Hermankzr";
    private static String XDA_POINTER = "xda";
    private static String POKEMON_POINTER_PACK = "Pokemon";

    private static String DEFAULT_PP_NAME = "Default Pointers";
    private static String CHRISTMAS_PP_NAME = "Christmas Pointers";
    private static String HEART_PP_NAME = "Heart Pointers";
    private static String GOOGLE_MI_PP_NAME = "GoogleMaterialIcons";
    private static String HERMANKZR_PP_NAME = "User Submitted: Hermankzr";
    private static String XDA_POINTER_NAME = "XDA Pointer";
    private static String POKEMON_PP_NAME = "Pokemon Pointers";

    private static String DIR_NAME_POINTERS = "pointers";
    private static String POKEMON_POINTERS_PACKAGE_NAME;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_pointer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String pointersFolder = getString(R.string.pointer_folder_path);
        String extSdDir = Environment.getExternalStorageDirectory().toString();
        mTargetPath = extSdDir + pointersFolder;
        mTag = "ManagePointers";
        mUtils = Helper.INSTANCE;

        mInstalledPointerPrefs = this.getSharedPreferences(getPackageName()+".installed_pointers", Context.MODE_PRIVATE);
        mInstalledPointerEditor = mInstalledPointerPrefs.edit();

        ListView pointersList = findViewById(R.id.pointerList);

        target = new File(mTargetPath);
        targetFiles = target.listFiles();

        ArrayList<String> list = new ArrayList<>();
        list.add(0, DEFAULT_PP_NAME);
        list.add(1, CHRISTMAS_PP_NAME);
        list.add(2, HEART_PP_NAME);
        list.add(3, GOOGLE_MI_PP_NAME);
        list.add(4, HERMANKZR_PP_NAME);
        list.add(5, XDA_POINTER_NAME);
        list.add(6, POKEMON_PP_NAME);

        pointerListAdapter = new PointerListAdapter(list, this);

        if (mInstalledPointerPrefs.getBoolean("first_launch", true)){
            Thread thread = new Thread(() -> {
                pointerListAdapter.copySpecificPointer(DEFAULT_POINTER_PACK);
                pointerListAdapter.copySpecificPointer(CHRISTMAS_POINTER_PACK);
                pointerListAdapter.copySpecificPointer(HEART_POINTER_PACK);
                pointerListAdapter.copySpecificPointer(GOOGLE_MI_PACK);
                pointerListAdapter.copySpecificPointer(HERMANKZR_PACK);
                pointerListAdapter.copySpecificPointer(XDA_POINTER);
                mInstalledPointerEditor.putBoolean(DEFAULT_POINTER_PACK, true).apply();
                mInstalledPointerEditor.putBoolean(CHRISTMAS_POINTER_PACK, true).apply();
                mInstalledPointerEditor.putBoolean(HEART_POINTER_PACK, true).apply();
                mInstalledPointerEditor.putBoolean(GOOGLE_MI_PACK, true).apply();
                mInstalledPointerEditor.putBoolean(HERMANKZR_PACK, true).apply();
                mInstalledPointerEditor.putBoolean(XDA_POINTER, true).apply();
                mInstalledPointerEditor.putBoolean(POKEMON_POINTER_PACK, false).apply();
            });
            thread.run();
            showHelpDialog();

            mInstalledPointerEditor.putBoolean("first_launch", false).apply();
        }

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setDefaults(R.xml.firebase_remote_defaults);
        firebaseRemoteConfig.fetch().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()){
                firebaseRemoteConfig.activateFetched();

                if (pointersList != null) {
                    POKEMON_POINTERS_PACKAGE_NAME = firebaseRemoteConfig.getString("pokemon_pack_play_store");
                    Log.d(ManagePointerActivity.class.getSimpleName(), "Pointer Package name: " + POKEMON_POINTERS_PACKAGE_NAME);
                    TransitionManager.beginDelayedTransition(findViewById(R.id.content_pointer_manage_root));
                    pointersList.setVisibility(View.VISIBLE);
                    pointersList.setAdapter(pointerListAdapter);
                    findViewById(R.id.progress_manage_pointer).setVisibility(View.INVISIBLE);
                }
            }
        }).addOnFailureListener(this, task -> {
            Log.d(ManagePointerActivity.class.getSimpleName(), "Can't connect to firebase, Using Default Package name instead.");
            POKEMON_POINTERS_PACKAGE_NAME = "tk.afterroot.pokmonpointers";
            Log.d(ManagePointerActivity.class.getSimpleName(), "Pointer Package name: " + POKEMON_POINTERS_PACKAGE_NAME);
            TransitionManager.beginDelayedTransition(findViewById(R.id.content_pointer_manage_root));
            pointersList.setVisibility(View.VISIBLE);
            pointersList.setAdapter(pointerListAdapter);
            findViewById(R.id.progress_manage_pointer).setVisibility(View.INVISIBLE);
        });

    }

    @Override
    protected void onResume() {
        pointerListAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_managepointer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.help:
                showHelpDialog();
                break;
        }
        return true;
    }

    private void showHelpDialog(){
        new MaterialDialog.Builder(this)
                .title("Help")
                .items("Click on INSTALL button to install all pointers from selected pack.",
                        "Click on DELETE button to delete all pointer from selected pack.",
                        "Click on Pointer Pack to view pointers from selected pack.",
                        "If you don't want to install full pointer pack, " +
                                "Click on Pointer Pack then in view pointers dialog " +
                                "click on pointer image to install only clicked pointer.")
                .positiveText(R.string.changelog_ok_button)
                .show();
    }

    private class PointerListAdapter extends BaseAdapter implements ListAdapter {

        private ArrayList<String> mArrayList;
        private Context mContext;
        private TextView installedState = null;

        PointerListAdapter(ArrayList<String> list, Context context){
            this.mArrayList = list;
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return mArrayList.size();
        }

        @Override
        public Object getItem(int i) {
            return mArrayList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        void copySpecificPointer(String nameStartsWith) {
            AssetManager am = getAssets();
            String[] files = null;

            try {
                files = am.list(DIR_NAME_POINTERS);
            } catch (IOException e){
                Log.e(mTag, e.getMessage());
            }
            assert files != null;
            for (String filename : files) {
                if (filename.startsWith(nameStartsWith)){
                    InputStream in;
                    OutputStream out;
                    try {
                        in = am.open(DIR_NAME_POINTERS + File.separator +filename);
                        out = new FileOutputStream(mTargetPath + filename);

                        byte[] bytes = new byte[1024];
                        int read;
                        while ((read = in.read(bytes)) != -1){
                            out.write(bytes, 0, read);
                        }

                        in.close();
                        out.flush();
                        out.close();
                        notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(mTag, e.getMessage());
                    }
                }
            }
        }

        void copyFile(File source, File destination) throws IOException{
            FileChannel sourceChannel = new FileInputStream(source).getChannel();
            FileChannel destinationChannel = new FileOutputStream(destination).getChannel();
            try {
                sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            } finally {
                try {
                    sourceChannel.close();
                    destinationChannel.close();
                } catch (IOException io){
                    io.printStackTrace();
                }
            }
        }

        void deletePack(String packName){
            for (File pointer : targetFiles){
                if (pointer.getName().startsWith(packName)){
                    pointer.delete();
                }
            }
            mInstalledPointerEditor.putBoolean(packName, false).apply();
        }

        void installPack(String packName){
            copySpecificPointer(packName);
            if (installedState != null) {
                installedState.setText(R.string.text_installed);
            }
            mInstalledPointerEditor.putBoolean(packName, true).apply();
            mUtils.showSnackBar(findViewById(R.id.content_pointer_manage_root), "Pointer Pack Installed.");
        }

        void showSpecificPointerDialog(String pointerPackName, String dialogTitle){
            AssetManager am = getAssets();
            String[] files = null;
            ArrayList<File> arrayList = new ArrayList<>();

            try {
                files = am.list(DIR_NAME_POINTERS);
            } catch (IOException e){
                Log.e(mTag, e.getMessage());
            }

            final PointerAdapter pointerAdapter = new PointerAdapter(mContext);
            assert files != null;
            for (String filename : files) {
                if (filename.startsWith(pointerPackName)){
                    InputStream in;
                    OutputStream out;
                    try {
                        in = am.open(DIR_NAME_POINTERS + File.separator + filename);
                        out = new FileOutputStream(getCacheDir() + File.separator + filename);
                        arrayList.add(new File(getCacheDir()+ File.separator + filename));

                        byte[] bytes = new byte[1024];
                        int read;
                        while ((read = in.read(bytes)) != -1){
                            out.write(bytes, 0, read);
                        }

                        in.close();
                        out.flush();
                        out.close();
                        notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(mTag, e.getMessage());
                    }
                }
            }
            for (File pointer : arrayList){
                if (pointer.getName().startsWith(pointerPackName)){
                    PointerAdapter.Companion.getItemList().add(pointer.getPath());
                }
            }
            MaterialDialog materialDialog = new MaterialDialog.Builder(mContext)
                    .title(dialogTitle)
                    .customView(R.layout.layout_grid_bottomsheet, false)
                    .show();
            materialDialog.setOnDismissListener(dialogInterface -> pointerAdapter.clear());
            View view1 = materialDialog.getCustomView();
            if (view1 != null) {
                final GridView gridView = view1.findViewById(R.id.grid_pointers);
                gridView.setAdapter(pointerAdapter);
                gridView.setOnItemClickListener((adapterView, view, i, l) -> {
                    File source = new File(pointerAdapter.getPath(i));
                    File targetFile = new File(mTargetPath + source.getName());
                    if (!targetFile.exists()){
                        try {
                            copyFile(source, targetFile);
                            Toast.makeText(mContext,
                                    String.format("%s %s", source.getName(), getString(R.string.text_installed)),
                                    Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(mContext, source.getName() + " is already installed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        void showPokemonDownloadDialog(){

            new MaterialDialog.Builder(ManagePointerActivity.this)
                    .title("Download Pokemon Pointers Pack")
                    .content("Pokemon Pointers is not installed. Do you want to download it now?")
                    .positiveText("Install")
                    .negativeText("No")
                    .onPositive((dialog, which) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + POKEMON_POINTERS_PACKAGE_NAME)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + POKEMON_POINTERS_PACKAGE_NAME)));
                        }
                    })
                    .show();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null){
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {
                    view = inflater.inflate(R.layout.manage_pointer_list_item, null);
                }
            }

            assert view != null;
            TextView listItemText = view.findViewById(R.id.item_pointer_pack_name);
            installedState = view.findViewById(R.id.text_installed_state);
            listItemText.setText(mArrayList.get(position));

            final Button installButton = view.findViewById(R.id.button_list_install);
            Button deleteButton = view.findViewById(R.id.button_list_delete);

            CardView listCard = view.findViewById(R.id.manage_pointers_list_card);

            switch (position){
                case 0:
                    if (!mInstalledPointerPrefs.getBoolean(DEFAULT_POINTER_PACK, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 1:
                    if (!mInstalledPointerPrefs.getBoolean(CHRISTMAS_POINTER_PACK, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 2:
                    if (!mInstalledPointerPrefs.getBoolean(HEART_POINTER_PACK, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 3:
                    if (!mInstalledPointerPrefs.getBoolean(GOOGLE_MI_PACK, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 4:
                    if (!mInstalledPointerPrefs.getBoolean(HERMANKZR_PACK, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 5:
                    if (!mInstalledPointerPrefs.getBoolean(XDA_POINTER, true)){
                        installedState.setText(R.string.text_not_installed);
                        installButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                    } else {
                        installButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                    }
                    break;
                case 6:
                    if (!mUtils.isAppInstalled(mContext, POKEMON_POINTERS_PACKAGE_NAME)){
                        deleteButton.setEnabled(false);
                        installButton.setEnabled(true);
                        installedState.setText("Pokemon Pointers app not found. Click INSTALL button to install Pack.");
                    } else {
                        if (!mInstalledPointerPrefs.getBoolean(POKEMON_POINTER_PACK, true)){
                            installedState.setText(R.string.text_not_installed);
                            installButton.setEnabled(true);
                            deleteButton.setEnabled(false);
                        } else {
                            installButton.setEnabled(false);
                            deleteButton.setEnabled(true);
                        }
                    }
                    break;
            }

            installButton.setOnClickListener(view12 -> {
                notifyDataSetChanged();
                switch (position){
                    case 0:
                        installPack(DEFAULT_POINTER_PACK);
                        break;
                    case 1:
                        installPack(CHRISTMAS_POINTER_PACK);
                        break;
                    case 2:
                        installPack(HEART_POINTER_PACK);
                        break;
                    case 3:
                        installPack(GOOGLE_MI_PACK);
                        break;
                    case 4:
                        installPack(HERMANKZR_PACK);
                        break;
                    case 5:
                        installPack(XDA_POINTER);
                        break;
                    case 6:
                        if (mUtils.isAppInstalled(mContext, POKEMON_POINTERS_PACKAGE_NAME)){
                            String[] files;
                            try {
                                AssetManager am = getPackageManager().getResourcesForApplication(POKEMON_POINTERS_PACKAGE_NAME).getAssets();
                                files = am.list(DIR_NAME_POINTERS);

                                assert files != null;
                                for (String filename : files) {
                                    if (filename.startsWith(POKEMON_POINTER_PACK)){
                                        InputStream in;
                                        OutputStream out;
                                        try {
                                            in = am.open(DIR_NAME_POINTERS + File.separator + filename);
                                            out = new FileOutputStream(mTargetPath + filename);

                                            byte[] bytes = new byte[1024];
                                            int read;
                                            while ((read = in.read(bytes)) != -1){
                                                out.write(bytes, 0, read);
                                            }

                                            in.close();
                                            out.flush();
                                            out.close();
                                            notifyDataSetChanged();
                                        } catch (Exception e) {
                                            Log.e(mTag, e.getMessage());
                                        }
                                    }
                                }
                            } catch (IOException e){
                                Log.e(mTag, e.getMessage());
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (installedState != null) {
                                installedState.setText(R.string.text_installed);
                            }
                            mInstalledPointerEditor.putBoolean(POKEMON_POINTER_PACK, true).apply();
                            notifyDataSetChanged();
                            mUtils.showSnackBar(findViewById(R.id.content_pointer_manage_root), "Pointer Pack Installed.");
                        } else {
                            showPokemonDownloadDialog();
                        }
                        break;
                }
                notifyDataSetChanged();
            });

            deleteButton.setOnClickListener(view13 -> {
                notifyDataSetChanged();
                targetFiles = target.listFiles();
                switch (position){
                    case 0:
                        deletePack(DEFAULT_POINTER_PACK);
                        break;
                    case 1:
                        deletePack(CHRISTMAS_POINTER_PACK);
                        break;
                    case 2:
                        deletePack(HEART_POINTER_PACK);
                        break;
                    case 3:
                        deletePack(GOOGLE_MI_PACK);
                        break;
                    case 4:
                        deletePack(HERMANKZR_PACK);
                        break;
                    case 5:
                        deletePack(XDA_POINTER);
                        break;
                    case 6:
                        for (File pointer : targetFiles){
                            if (pointer.getName().startsWith(POKEMON_POINTER_PACK)){
                                pointer.delete();
                                notifyDataSetChanged();
                            }
                        }
                        mInstalledPointerEditor.putBoolean(POKEMON_POINTER_PACK, false).apply();
                        break;
                }
                notifyDataSetChanged();
                mUtils.showSnackBar(findViewById(R.id.content_pointer_manage_root),"Pointer Pack Deleted.");
            });

            listCard.setOnClickListener(view14 -> {
                switch (position){
                    case 0:
                        showSpecificPointerDialog(DEFAULT_POINTER_PACK, DEFAULT_PP_NAME);
                        break;
                    case 1:
                        showSpecificPointerDialog(CHRISTMAS_POINTER_PACK, CHRISTMAS_PP_NAME);
                        break;
                    case 2:
                        showSpecificPointerDialog(HEART_POINTER_PACK, HERMANKZR_PP_NAME);
                        break;
                    case 3:
                        showSpecificPointerDialog(GOOGLE_MI_PACK, GOOGLE_MI_PP_NAME);
                        break;
                    case 4:
                        showSpecificPointerDialog(HERMANKZR_PACK, HEART_PP_NAME);
                        break;
                    case 5:
                        showSpecificPointerDialog(XDA_POINTER, XDA_POINTER_NAME);
                        break;
                    case 6:
                        if (mUtils.isAppInstalled(mContext, POKEMON_POINTERS_PACKAGE_NAME)){
                            String[] files;
                            ArrayList<File> arrayList = new ArrayList<>();
                            try {
                                AssetManager am = getPackageManager().getResourcesForApplication(POKEMON_POINTERS_PACKAGE_NAME).getAssets();
                                files = am.list(DIR_NAME_POINTERS);

                                final PointerAdapter pointerAdapter = new PointerAdapter(mContext);
                                assert files != null;
                                for (String filename : files) {
                                    if (filename.startsWith(POKEMON_POINTER_PACK)){
                                        String target1 = getCacheDir() + File.separator + filename;
                                        if (!new File(target1).exists()){
                                            InputStream in;
                                            OutputStream out;
                                            try {
                                                in = am.open(DIR_NAME_POINTERS + File.separator + filename);
                                                out = new FileOutputStream(target1);
                                                arrayList.add(new File(target1));

                                                byte[] bytes = new byte[1024];
                                                int read;
                                                while ((read = in.read(bytes)) != -1){
                                                    out.write(bytes, 0, read);
                                                }

                                                in.close();
                                                out.flush();
                                                out.close();
                                                notifyDataSetChanged();
                                            } catch (Exception e) {
                                                Log.e(mTag, e.getMessage());
                                            }
                                        } else {
                                            arrayList.add(new File(target1));
                                        }
                                    }
                                }
                                for (File pointer : arrayList){
                                    if (pointer.getName().startsWith(POKEMON_POINTER_PACK)){
                                        PointerAdapter.Companion.getItemList().add(pointer.getPath());
                                    }
                                }
                                MaterialDialog materialDialog = new MaterialDialog.Builder(mContext)
                                        .title(POKEMON_PP_NAME)
                                        .customView(R.layout.layout_grid_bottomsheet, false)
                                        .show();
                                materialDialog.setOnDismissListener(dialogInterface -> pointerAdapter.clear());
                                View view1 = materialDialog.getCustomView();
                                if (view1 != null) {
                                    final GridView gridView = view1.findViewById(R.id.grid_pointers);
                                    gridView.setAdapter(pointerAdapter);
                                    gridView.setOnItemClickListener((adapterView, view2, i, l) -> {
                                        File source = new File(pointerAdapter.getPath(i));
                                        File targetFile = new File(mTargetPath + source.getName());
                                        if (!targetFile.exists()){
                                            try {
                                                copyFile(source, targetFile);
                                                Toast.makeText(mContext,
                                                        String.format("%s %s", source.getName(), getString(R.string.text_installed)),
                                                        Toast.LENGTH_SHORT).show();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(mContext, source.getName() + " is already installed.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (PackageManager.NameNotFoundException ignored) {

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            showPokemonDownloadDialog();
                        }
                        break;
                }
            });
            return view;
        }
    }

}
