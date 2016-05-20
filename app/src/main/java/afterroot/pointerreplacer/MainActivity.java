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

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

import static afterroot.pointerreplacer.Utils.getDpi;
import static afterroot.pointerreplacer.Utils.loadToBottomSheetGrid;
import static afterroot.pointerreplacer.Utils.showSnackbar;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ColorChooserDialog.ColorCallback {

    int mPointersVersion, latestPointerVersion, oldColor;
    DiscreteSeekBar mPointerSizeBar, mPaddingBar;
    ImageView mPointerSelected, mCurrentPointer;
    TextView mTextSize, mPaddingSize, mTextCurrentPointer, mTextSelectedPointer;
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;
    String mPointersFolder, mExtSdDir, mTargetPath, mTag, mPointerPreviewPath;
    Boolean isUseMDCC;
    Toolbar mToolbar;
    File mFolderPointers; GridView mGridView;
    BottomSheetLayout bottomSheet;
    ActionBarDrawerToggle toggle;
    DrawerLayout drawer;
    String[] PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE};
    PermissionChecker permissionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        setToggle();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
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

        initialize();

    }

    static final int REQUEST_CODE = 0;
    public void checkPermissions(){
        permissionChecker = new PermissionChecker(this);
        if (permissionChecker.lacksPermissions(PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
        }
    }

    boolean PERMISSION_GRANTED;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE:
                PERMISSION_GRANTED = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setToggle(){
        toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onResume(){
        super.onResume();
        checkPermissions();
        getPointer();
        setSeekbar();
    }

    @SuppressLint("CommitPrefEdits")
    public void initialize(){
        /**Load SharedPreferences**/
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        setInts();
        setStrings();
        findViews();
        loadMethods();
    }

    /**
     * Load or Set Ints
     */
    public void setInts(){
        latestPointerVersion = 3;
    }

    /**
     * Load or Set Strings
     */
    public void setStrings(){
        mTag = getString(R.string.app_name);
        mPointersFolder = getString(R.string.pointerFolderName);
        mExtSdDir = Environment.getExternalStorageDirectory().toString();
        mTargetPath = mExtSdDir + mPointersFolder;
        mPointerPreviewPath = getFilesDir().getPath()+"/pointerPreview.png";
    }

    /**Find Views**/
    public void findViews(){
        mPointerSizeBar = (DiscreteSeekBar) findViewById(R.id.seekBar);
        mPaddingBar = (DiscreteSeekBar) findViewById(R.id.seekBarPadding);
        mTextSize = (TextView) findViewById(R.id.textView_size);
        mPaddingSize = (TextView) findViewById(R.id.textPadding);
        mTextCurrentPointer = (TextView) findViewById(R.id.textCurrentPointer);
        mTextSelectedPointer = (TextView) findViewById(R.id.textSelectedPointer);
        mPointerSelected = (ImageView) findViewById(R.id.pointerSelected);
        mCurrentPointer = (ImageView) findViewById(R.id.image_current_pointer);
    }

    /**
     * Load Methods
     */
    public void loadMethods(){
        showChangelog();
        createPointersFolder();
        newPointerCopier();
        getPointer();
        setSeekbar();
    }

    public void showPreview(boolean isStartPreview){
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
        if (isStartPreview){
            startActivity(new Intent(MainActivity.this, PointerPreview.class));
        }
    }

    /**
     * Show A Changelog Dialog
     */
    public void showChangelog() {
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) cl.getLogDialog().show();
    }

    /**
     * Create Pointers Folder
     */
    public void createPointersFolder(){
        mFolderPointers = new File(mTargetPath);
        try {
            if (!mFolderPointers.exists()) {
                mFolderPointers.mkdirs();
                copyAssets();
            }
            File dotnomedia = new File(mExtSdDir+"/Pointer Replacer/.nomedia");
            if (!dotnomedia.exists()){
                dotnomedia.createNewFile();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    /**
     * Decides Whether to Copy pointer or not when new pointers are added.
     */
    public void newPointerCopier(){
        String textPointerCopied = "Pointers Copied to ";
        mPointersVersion = mSharedPreferences.getInt(
                getString(R.string.key_pointersVersion),
                latestPointerVersion);

        if (mPointersVersion == latestPointerVersion){
            copyAssets();
            mEditor.putInt(getString(R.string.key_pointersVersion), ++mPointersVersion);
            mEditor.apply();
            showSnackbar(findViewById(R.id.main_layout),"New "+textPointerCopied + mTargetPath);
        }
        try {
            if (!mFolderPointers.exists()) {
                mFolderPointers.mkdir();
                copyAssets();
                showSnackbar(findViewById(R.id.main_layout), textPointerCopied + mTargetPath);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * get current current pointer
     */
    public void getPointer(){
        try {
            String pointerPath = mSharedPreferences.getString(getString(R.string.key_pointerPath), null);
            Drawable d = Drawable.createFromPath(pointerPath);
            mCurrentPointer.setImageDrawable(d);

            mPointerSelected.setImageDrawable(
                    Drawable.createFromPath(mSharedPreferences.getString(getString(R.string.key_selectedPointerPath), null)));
        } catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    public void showPointerChooser(View view) {
        mToolbar.setTitle("Choose Pointer");
        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomSheet);
        bottomSheet.showWithSheetView(LayoutInflater
                .from(getApplicationContext())
                .inflate(R.layout.gridview_bottomsheet, bottomSheet, false));
        bottomSheet.addOnSheetDismissedListener(new OnSheetDismissedListener() {
            @Override
            public void onDismissed(BottomSheetLayout bottomSheetLayout) {
                mToolbar.setTitle(getString(R.string.app_name));
                setToggle();
            }
        });
        mGridView = (GridView) findViewById(R.id.bs_gridView);

        loadToBottomSheetGrid(this, mGridView, mTargetPath, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mEditor.putString(getString(R.string.key_selectedPointerPath), Utils.PointerAdapter.getPath(i)).apply();
                mPointerSelected.setImageDrawable(Drawable.createFromPath(Utils.PointerAdapter.getPath(i)));
                bottomSheet.dismissSheet();
            }
        });
        DrawerArrowDrawable drawerArrowDrawable = new DrawerArrowDrawable(this);
        drawerArrowDrawable.setProgress(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white, getTheme()));
        } else {
            drawerArrowDrawable.setColor(getResources().getColor(android.R.color.white));
        }
        mToolbar.setNavigationIcon(drawerArrowDrawable);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheet.dismissSheet();
            }
        });
    }

    public void setPointerImageParams(int size, int padding, boolean isApplyPadding){
        mPointerSelected.setLayoutParams(new LinearLayout.LayoutParams(size,size));
        if (isApplyPadding){
            mPointerSelected.setPadding(padding, padding, padding, padding);
        }
    }

    /**
     * Set initial values to seekbar
     */
    public void setSeekbar(){
        if (getDpi(this) <= 240){
            mPointerSizeBar.setMin(49);
        } else if (getDpi(this) >= 240){
            mPointerSizeBar.setMin(66);
        }

        mPointerSelected.setLayoutParams(new LinearLayout.LayoutParams(mPointerSizeBar.getProgress(), mPointerSizeBar.getProgress()));
        String maxSize = mSharedPreferences.getString(getString(R.string.key_maxPointerSize), "100");
        mPointerSizeBar.setMax(Integer.valueOf(maxSize));
        int prefGridSize = mSharedPreferences.getInt(getString(R.string.key_pointerSize), mPointerSizeBar.getMin());
        setSeekBarProgress(prefGridSize);
        final String textSize = "Size: %d*%d";
        mTextSize.setText(String.format(Locale.US, textSize, prefGridSize, prefGridSize));
        mPointerSizeBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int imageSize;
            @Override
            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int size, boolean b) {
                mEditor.putInt(getString(R.string.key_pointerSize), size).apply();
                mTextSize.setText(String.format(textSize, size, size));
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
                mPaddingSize.setText(String.format(Locale.US, "Padding: %d", value));
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
    }

    /**
     * @param progress Integer value to be set as progress.
     */
    public void setSeekBarProgress(int progress){
        mPointerSizeBar.setProgress(progress);
    }

    /**Show color picker dialog.**/
    public void showColorPicker() {
        final String keyOldColor = getString(R.string.key_oldColor);
        int old_color = mSharedPreferences.getInt(keyOldColor, -1);
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, old_color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                showSnackbar(findViewById(R.id.main_layout), "Pointer Color not Changed.");
            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mPointerSelected.setColorFilter(color);
                showSnackbar(findViewById(R.id.main_layout), "Pointer Color Changed.");
                mEditor.putInt(keyOldColor, color);
                mEditor.apply();
            }
        });
        dialog.show();
    }

    /**
     * @throws IOException
     */
    public void confirm() throws IOException {
        String pointerPath = getFilesDir().getPath()+"/pointer.png";
        mEditor.putString(getString(R.string.key_pointerPath), pointerPath);
        mEditor.apply();
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
            mCurrentPointer.setImageDrawable(d);
            showRebootDialog();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**Show a reboot confirm dialog**/
    public void showRebootDialog(){
        String textReboot = getString(R.string.reboot);
        new MaterialDialog.Builder(this)
                .title(textReboot)
                .theme(Theme.DARK)
                .content("Do you want to Reboot?")
                .positiveText(textReboot)
                .negativeText("Cancel")
                .neutralText("Soft "+ textReboot)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                            process.waitFor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "busybox killall system_server"});
                            process.waitFor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
                setSeekBarProgress(progress + 1);
                break;
            case R.id.butMinus:
                setSeekBarProgress(progress - 1);
                break;
            case R.id.butPaddingPlus:
                mPaddingBar.setProgress(padding + 1);
                break;
            case R.id.butPaddingMinus:
                mPaddingBar.setProgress(padding - 1);
                break;
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
        mPointerSelected.setColorFilter(selectedColor);
        showSnackbar(findViewById(R.id.main_layout), "Pointer Color Changed.");
        mEditor.putInt("OLD_COLOR", selectedColor);
        mEditor.apply();
    }

    public int getOldColor(){
        oldColor = mSharedPreferences.getInt(getString(R.string.key_oldColor), -1);
        return oldColor;
    }

    public void showPointerColorChooser() {
        new ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(false)
                .dynamicButtonColor(false)
                .preselect(getOldColor())
                .show();
    }

    /**Copy Assets**/
    public void copyAssets() {
        AssetManager am = getAssets();
        String[] files = null;

        try {
            files = am.list("");
        } catch (IOException e){
            Log.e(mTag, e.getMessage());
        }
        assert files != null;
        for (String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = am.open(filename);
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
        File file = new File(mTargetPath+"xposed_init");
        if (file.exists()) {
            file.delete();
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

    public void showConfirmDialog(){
        new MaterialDialog.Builder(this)
                .title("Confirm Copy")
                .theme(Theme.DARK)
                .content("Do you want to copy pointers to\n" +
                        mTargetPath + "\n"+
                        "Do only if pointers are not copied automatically.")
                .positiveText("Yes")
                .neutralText("No")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        copyAssets();
                        showSnackbar(findViewById(R.id.main_layout), "Pointers Copied to " + mTargetPath);
                    }
                }).show();
    }

    public void showSureDialog(){
        Drawable drawable = mPointerSelected.getDrawable();
        new MaterialDialog.Builder(this)
                .title("Are You Sure?")
                .theme(Theme.DARK)
                .content("Do you want to apply this pointer?")
                .positiveText("Yes")
                .negativeText("No")
                .neutralText("Pointer Preview")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        showPreview(true);
                    }
                })
                .maxIconSize(50)
                .icon(drawable)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            confirm();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        showSnackbar(findViewById(R.id.main_layout), "Pointer Applied ");
                    }
                }).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
                if (getDpi(this) <= 240){
                    setSeekBarProgress(49);
                } else if (getDpi(this) >= 240){
                    setSeekBarProgress(66);
                }
                mPaddingBar.setProgress(0);
            } return true;
            case R.id.viewPreview:
                showPreview(true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.action_changelog:
                ChangeLog cl = new ChangeLog(this);
                cl.getFullLogDialog().show();
                break;
            case R.id.action_copy_pointers:
                showConfirmDialog();
                break;
            case R.id.change_color:
                isUseMDCC = mSharedPreferences.getBoolean(getString(R.string.key_useMDCC), true);
                if (isUseMDCC){
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
                showSnackbar(findViewById(R.id.main_layout), "Color changed to Default");
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.preview:
                showPreview(true);
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
