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

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ColorChooserDialog.ColorCallback, FileChooserDialog.FileCallback {

    private static final int REQUEST_CODE = 0;
    private int latestPointerVersion = 5;
    private DiscreteSeekBar mPointerSizeBar, mPaddingBar, mAlphaBar;
    private ImageView mPointerSelected, mCurrentPointer;
    private TextView mTextSize, mPaddingSize, mTextAlpha;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private String mExtSdDir;
    private String mTargetPath;
    private String mTag;
    private String mPointerPreviewPath;
    private Toolbar mToolbar;
    private File mFolderPointers;
    private BottomSheetLayout bottomSheet;
    private DrawerLayout drawer;
    private DrawerArrowDrawable drawerArrowDrawable;
    private Utils mUtils;
    private RelativeLayout mRootLayout;
    private String[] PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        drawer = findViewById(R.id.drawer_layout);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString("Device_Name", Build.DEVICE);
        bundle.putString("AndroidVersion", Build.VERSION.CODENAME);
        analytics.logEvent("DeviceInfo", bundle);

        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id));

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null){
            navigationView.setNavigationItemSelectedListener(this);
            int tintlistid;
            tintlistid = R.color.nav_state_list;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                navigationView.setItemIconTintList(getResources().getColorStateList(tintlistid, getTheme()));
            } else {
                navigationView.setItemIconTintList(getResources().getColorStateList(tintlistid));
            }
        }
        checkPermissions();

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));

        initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageLoader.getInstance().destroy();
    }

    private void checkPermissions(){
        PermissionChecker permissionChecker = new PermissionChecker(this);
        if (permissionChecker.lacksPermissions(PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE:
                boolean PERMISSION_GRANTED = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setToggle(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white, getTheme()));
        } else {
            drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white));
        }
        mToolbar.setNavigationIcon(drawerArrowDrawable);
        mToolbar.setNavigationOnClickListener(view -> drawer.openDrawer(GravityCompat.START));
    }

    @Override
    public void onResume(){
        super.onResume();
        checkPermissions();
        getPointer();
        setSeekbars();
    }

    private InterstitialAd mInterstitialAd;
    @SuppressLint("CommitPrefEdits")
    private void initialize(){
        /*Load SharedPreferences**/
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();
        
        mUtils = new Utils();

        drawerArrowDrawable = new DrawerArrowDrawable(this);

        setStrings();
        findViews();
        loadMethods();

        AdView mAdView = findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_1_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    /**
     * Load or Set Strings
     */
    private void setStrings(){
        mTag = getString(R.string.app_name);
        String pointersFolder = getString(R.string.pointerFolderName);
        mExtSdDir = Environment.getExternalStorageDirectory().toString();
        mTargetPath = mExtSdDir + pointersFolder;
        mPointerPreviewPath = getFilesDir().getPath()+"/pointerPreview.png";
    }

    /**Find Views**/
    private void findViews(){
        mPointerSizeBar = findViewById(R.id.seekBar);
        mPaddingBar = findViewById(R.id.seekBarPadding);
        mAlphaBar = findViewById(R.id.seekBarAlpha);
        mTextSize = findViewById(R.id.textView_size);
        mTextAlpha = findViewById(R.id.textAlpha);
        mPaddingSize = findViewById(R.id.textPadding);
        mPointerSelected = findViewById(R.id.pointerSelected);
        mCurrentPointer = findViewById(R.id.image_current_pointer);
        mRootLayout = findViewById(R.id.main_layout);
    }

    /**
     * Load Methods
     */
    private void loadMethods(){
        showChangelog();
        createPointersFolder();
        getPointer();
        setSeekbars();
        setToggle();
    }

    private void showPreview(){
        try {
            Bitmap bitmap = loadBitmapFromView(mPointerSelected);
            File file = new File(mPointerPreviewPath);
            Runtime.getRuntime().exec("chmod 666 "+mPointerPreviewPath);
            FileOutputStream out;
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        startActivity(new Intent(MainActivity.this, PointerPreview.class));
    }

    /**
     * Show A Changelog Dialog
     */
    private void showChangelog() {
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) cl.getLogDialog().show();
    }

    /**
     * Create Pointers Folder
     */
    private void createPointersFolder(){
        mFolderPointers = new File(mTargetPath);
        try {
            if (!mFolderPointers.exists()) {
                mFolderPointers.mkdirs();
                showInstallPointersDialog();
            }
            if (mFolderPointers.listFiles().length <= 0){
                showInstallPointersDialog();
            }

            File dotnomedia = new File(mExtSdDir+"/Pointer Replacer/.nomedia");
            if (!dotnomedia.exists()){
                dotnomedia.createNewFile();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void showInstallPointersDialog(){
        new MaterialDialog.Builder(this)
                .title("Install Pointers")
                .content("No Pointers installed. Please install some pointers")
                .positiveText("Install Pointers")
                .onPositive((dialog, which) -> startActivity(new Intent(MainActivity.this, ManagePointerActivity.class))).show();
    }

    /**
     * Decides Whether to Copy pointer or not when new pointers are added.
     */
    private void newPointerCopier(){
        String textPointerCopied = getString(R.string.text_pointers_copied) + mTargetPath;
        int pointersVersion = mSharedPreferences.getInt(
                getString(R.string.key_pointersVersion),
                latestPointerVersion);

        if (pointersVersion == latestPointerVersion){
            copyAssets();
            mEditor.putInt(getString(R.string.key_pointersVersion), ++pointersVersion);
            mEditor.apply();
            mUtils.showSnackbar(mRootLayout,getString(R.string.text_new) +textPointerCopied);
        }
        try {
            if (!mFolderPointers.exists()) {
                mFolderPointers.mkdir();
                copyAssets();
                mUtils.showSnackbar(mRootLayout, textPointerCopied);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * get current current pointer
     */
    private void getPointer(){
        try {
            String pointerPath = mSharedPreferences.getString(getString(R.string.key_pointerPath), null);
            String selectedPointerPath = mSharedPreferences.getString(getString(R.string.key_selectedPointerPath), null);
            if (pointerPath != null) {
                mCurrentPointer.setImageDrawable(Drawable.createFromPath(pointerPath));
                hideView(R.id.textNoPointerApplied);
            } else {
                showView(R.id.textNoPointerApplied);
                hideView(mCurrentPointer);
            }

            if (selectedPointerPath != null) {
                mPointerSelected.setImageDrawable(Drawable.createFromPath(selectedPointerPath));
            }
        } catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    private void hideView(@IdRes int id){
        try {
            findViewById(id).setVisibility(View.GONE);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    private void showView(@IdRes int id){
        try {
            findViewById(id).setVisibility(View.VISIBLE);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }

    }

    private void hideView(View view){
        try {
            view.setVisibility(View.GONE);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    private void showView(View view){
        try {
            view.setVisibility(View.VISIBLE);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }

    }

    public void showPointerChooser(View view) {
        File[] files = new File(mTargetPath).listFiles();
        if (files.length > 0){
            final Utils.PointerAdapter pointerAdapter = new Utils.PointerAdapter(this);
            ObjectAnimator.ofFloat(drawerArrowDrawable, "progress", 1).start();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white, getTheme()));
            } else {
                drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white));
            }
            mToolbar.setNavigationIcon(drawerArrowDrawable);
            mToolbar.setNavigationOnClickListener(view1 -> bottomSheet.dismissSheet());

            mToolbar.setTitle(R.string.text_choose_pointer);
            bottomSheet = findViewById(R.id.bottomSheet);
            bottomSheet.showWithSheetView(LayoutInflater
                    .from(getApplicationContext())
                    .inflate(R.layout.gridview_bottomsheet, bottomSheet, false));
            bottomSheet.addOnSheetDismissedListener(bottomSheetLayout -> {
                mToolbar.setTitle(getString(R.string.app_name));
                ObjectAnimator.ofFloat(drawerArrowDrawable, "progress", 0).start();
                pointerAdapter.clear();
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view12) {
                        drawer.openDrawer(GravityCompat.START);
                    }
                });
            });
            GridView gridView = findViewById(R.id.bs_gridView);

            mUtils.loadToBottomSheetGrid(this, gridView, mTargetPath, (adapterView, view13, i, l) -> {
                mEditor.putString(getString(R.string.key_selectedPointerPath), pointerAdapter.getPath(i)).apply();
                mPointerSelected.setImageDrawable(Drawable.createFromPath(pointerAdapter.getPath(i)));
                bottomSheet.dismissSheet();
                Log.d(mTag, "Selected Pointer Path: "+pointerAdapter.getPath(i));
            });
            if (gridView != null) {
                gridView.setOnItemLongClickListener((adapterView, view14, i, l) -> {
                    bottomSheet.dismissSheet();
                    final File file = new File(pointerAdapter.getPath(i));
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(getString(R.string.text_delete) + file.getName())
                            .content(R.string.text_delete_confirm)
                            .positiveText(R.string.text_yes)
                            .onPositive((dialog, which) -> file.delete())
                            .negativeText(R.string.text_no)
                            .show();

                    return false;
                });
            }
        } else {
            showInstallPointersDialog();
        }
    }

    private void setPointerImageParams(int size, int padding, boolean isApplyPadding){
        mPointerSelected.setLayoutParams(new LinearLayout.LayoutParams(size,size));
        if (isApplyPadding){
            mPointerSelected.setPadding(padding, padding, padding, padding);
        }
    }

    private int getMinSize(){
        if (mUtils.getDpi(this) <= 240){
            return 49;
        } else {
            return 66;
        }
    }

    /**
     * Set initial values to seekbar
     */
    private void setSeekbars(){
        String maxSize = mSharedPreferences.getString(getString(R.string.key_maxPointerSize), "100");
        String maxPadding = mSharedPreferences.getString(getString(R.string.key_maxPaddingSize), "25");
        int alpha = mSharedPreferences.getInt("pointerAlpha", 255);
        int pointerSize = mSharedPreferences.getInt(getString(R.string.key_pointerSize), mPointerSizeBar.getMin());
        int padding = mSharedPreferences.getInt(getString(R.string.key_pointerPadding), 0);
        final String formatTextSize = "%s: %d*%d ";
        final String formatPadding = "| %s: %d ";

        mPointerSizeBar.setMin(getMinSize());

        RelativeLayout alphaBarContainer = findViewById(R.id.alpha_bar_container);
        if (mSharedPreferences.getBoolean(getString(R.string.key_EnablePointerAlpha), false)) {
            if (alphaBarContainer != null) {
                showView(alphaBarContainer);
                showView(mTextAlpha);
            }
        } else{
            if (alphaBarContainer != null) {
                hideView(alphaBarContainer);
                hideView(mTextAlpha);
            }
        }
        mPointerSelected.setAlpha(alpha);

        //pointer size
        mPointerSizeBar.setMax(Integer.valueOf(maxSize));
        mTextSize.setText(String.format(Locale.US, formatTextSize, getString(R.string.text_size), pointerSize, pointerSize));

        //pointer padding
        mPaddingBar.setMax(Integer.valueOf(maxPadding));
        mPaddingBar.setProgress(padding);
        mPaddingSize.setText(String.format(Locale.US, formatPadding, getString(R.string.text_padding), padding));

        //pointer alpha
        mTextAlpha.setText(String.format(Locale.US, formatPadding, getString(R.string.text_alpha), alpha));
        mAlphaBar.setProgress(alpha);

        setPointerImageParams(pointerSize, padding, true);
        setPointerSizeBarProgress(pointerSize);

        mPointerSizeBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int imageSize;
            @Override
            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int size, boolean b) {
                mEditor.putInt(getString(R.string.key_pointerSize), size).apply();
                mTextSize.setText(String.format(Locale.US, formatTextSize, getString(R.string.text_size), size, size));
                imageSize = size;
                setPointerImageParams(size, mPaddingBar.getProgress() ,false);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                setPointerImageParams(imageSize, mPaddingBar.getProgress() ,false);
            }
        });

        mPaddingBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int imagePadding;
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                mEditor.putInt(getString(R.string.key_pointerPadding), value).apply();
                mPaddingSize.setText(String.format(Locale.US, formatPadding, getString(R.string.text_padding), value));
                setPointerImageParams(mPointerSizeBar.getProgress(), value, true);
                imagePadding = value;
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                setPointerImageParams(mPointerSizeBar.getProgress(), imagePadding, true);
            }
        });

        mAlphaBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                mEditor.putInt("pointerAlpha", value).apply();
                mTextAlpha.setText(String.format(Locale.US, formatPadding, getString(R.string.text_alpha),value));
                mPointerSelected.setAlpha(value);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });
    }

    /**
     * @param progress Integer value to be set as progress.
     */
    private void setPointerSizeBarProgress(int progress){
        mPointerSizeBar.setProgress(progress);
    }

    /**Show color picker dialog.**/
    private void showColorPicker() {
        final String keyOldColor = getString(R.string.key_oldColor);
        int old_color = mSharedPreferences.getInt(keyOldColor, -1);
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, old_color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                mUtils.showSnackbar(mRootLayout, getString(R.string.text_color_not_changed));
            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mPointerSelected.setColorFilter(color);
                mUtils.showSnackbar(mRootLayout, getString(R.string.text_color_changed));
                mEditor.putInt(keyOldColor, color);
                mEditor.apply();
            }
        });
        dialog.show();
    }

    /**
     * @throws IOException
     */
    private void applyPointer() throws IOException {
        String pointerPath = getFilesDir().getPath()+"/pointer.png";
        mEditor.putString(getString(R.string.key_pointerPath), pointerPath).apply();
        Bitmap bitmap = loadBitmapFromView(mPointerSelected);
        File file = new File(pointerPath);
        Runtime.getRuntime().exec("chmod 666 "+pointerPath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Drawable d = Drawable.createFromPath(pointerPath);
            showView(mCurrentPointer);
            hideView(R.id.textNoPointerApplied);
            mCurrentPointer.setImageDrawable(d);
            showRebootDialog();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**Show a reboot confirm dialog**/
    private void showRebootDialog(){
        new MaterialDialog.Builder(this)
                .title(R.string.reboot)
                .theme(Theme.DARK)
                .content(R.string.text_reboot_confirm)
                .positiveText(R.string.reboot)
                .negativeText(R.string.text_no)
                .neutralText(R.string.text_soft_reboot)
                .onPositive((dialog, which) -> {
                    try {
                        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                        process.waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .onNeutral((dialog, which) -> {
                    try {
                        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "busybox killall system_server"});
                        process.waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private Bitmap loadBitmapFromView(View v) {
        final int w = v.getWidth();
        final int h = v.getHeight();
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    public void changeSeekVal(View seek) {
        int progress = mPointerSizeBar.getProgress();
        int padding = mPaddingBar.getProgress();

        switch (seek.getId()){
            case R.id.butPlus:
                setPointerSizeBarProgress(progress + 1);
                break;
            case R.id.butMinus:
                setPointerSizeBarProgress(progress - 1);
                break;
            case R.id.butPaddingPlus:
                mPaddingBar.setProgress(padding + 1);
                break;
            case R.id.butPaddingMinus:
                mPaddingBar.setProgress(padding - 1);
                break;
            case R.id.butAlphaMinus:
                mAlphaBar.setProgress(mAlphaBar.getProgress() - 1);
                break;
            case R.id.butAlphaPlus:
                mAlphaBar.setProgress(mAlphaBar.getProgress() + 1);
                break;
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
        mPointerSelected.setColorFilter(selectedColor);
        mUtils.showSnackbar(mRootLayout, getString(R.string.text_color_changed));
        mEditor.putInt(getString(R.string.key_oldColor), selectedColor);

        mEditor.apply();
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }

    private int getOldColor(){
        return mSharedPreferences.getInt(getString(R.string.key_oldColor), -1);
    }

    private void showPointerColorChooser() {
        new ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(false)
                .dynamicButtonColor(false)
                .preselect(getOldColor())
                .show();
    }

    /**Copy Assets**/
    private void copyAssets() {
        AssetManager am = getAssets();
        String[] files = null;

        try {
            files = am.list("pointers");
        } catch (IOException e){
            Log.e(mTag, e.getMessage());
        }
        assert files != null;
        for (String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = am.open("pointers/"+filename);
                out = new FileOutputStream(mTargetPath + filename);

                byte[] bytes = new byte[1024];
                int read;
                while ((read = in.read(bytes)) != -1){
                    out.write(bytes, 0, read);
                }

                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e(mTag, e.getMessage());
            }
        }

        File dotnomedia = new File(mExtSdDir+"/Pointer Replacer/.nomedia");
        if (!dotnomedia.exists()){
            try {
                dotnomedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showConfirmDialog(){
        new MaterialDialog.Builder(this)
                .title(R.string.copy_pointers)
                .theme(Theme.DARK)
                .content(getString(R.string.text_copy_confirm_1) +
                        mTargetPath + "\n"+
                        getString(R.string.text_copy_confirm_2))
                .positiveText(R.string.text_yes)
                .negativeText(R.string.text_no)
                .onPositive((dialog, which) -> {
                    copyAssets();
                    mUtils.showSnackbar(mRootLayout, getString(R.string.text_pointers_copied) + mTargetPath);
                }).show();
    }

    private void showSureDialog(){
        Drawable drawable = mPointerSelected.getDrawable();
        new MaterialDialog.Builder(this)
                .title(R.string.apply_pointer)
                .theme(Theme.DARK)
                .content(R.string.text_apply_pointer_confirm)
                .positiveText(R.string.text_yes)
                .negativeText(R.string.text_no)
                .neutralText(R.string.title_activity_pointer_preview)
                .onNeutral((dialog, which) -> showPreview())
                .maxIconSize(50)
                .icon(drawable)
                .onPositive((dialog, which) -> {
                    try {
                        applyPointer();
                        if (mInterstitialAd.isLoaded()){
                            mInterstitialAd.show();
                        } else {
                            Log.d(MainActivity.class.getSimpleName(), "The interstitial wasn't loaded yet.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mUtils.showSnackbar(mRootLayout, getString(R.string.text_pointer_applied));
                }).show();
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        mUtils.showSnackbar(mRootLayout, getString(R.string.text_selected_pointer)+ ": "+file.getName());
        mPointerSelected.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));

        if (new File(mTargetPath+file.getName()).exists()){
            mUtils.showSnackbar(mRootLayout, getString(R.string.text_pointer_exists));
        } else {
            try {
                mUtils.copyFile(file, new File(mTargetPath+file.getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.reset_grid:{
                setPointerSizeBarProgress(getMinSize());
                mPaddingBar.setProgress(0);
            }
            return true;
            case R.id.viewPreview:
                showPreview();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_changelog:
                ChangeLog cl = new ChangeLog(this);
                cl.getFullLogDialog().show();
                break;
            case R.id.change_color:
                if (mSharedPreferences.getBoolean(getString(R.string.key_useMDCC), true)){
                    showPointerColorChooser();
                } else {
                    showColorPicker();
                }
                break;
            case R.id.apply_pointer:
                showSureDialog();
                break;
            case R.id.reboot:
                showRebootDialog();
                break;
            case R.id.reset_color:
                mPointerSelected.setColorFilter(null);
                mUtils.showSnackbar(mRootLayout, getString(R.string.text_color_changed_default));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.preview:
                showPreview();
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.import_pointer:
                new FileChooserDialog.Builder(this)
                        .mimeType("image/*")
                        .show();
                break;
            case R.id.manage_pointers:
                startActivity(new Intent(this, ManagePointerActivity.class));
                break;
        }

        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}