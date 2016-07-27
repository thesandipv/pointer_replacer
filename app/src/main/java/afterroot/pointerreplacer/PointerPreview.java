/*
 * Copyright (C) 2016 Sandip Vaghela
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

/*
 * Copyright (C) 2016 Sandip Vaghela
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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PointerPreview extends AppCompatActivity implements ColorChooserDialog.ColorCallback {
    private RelativeLayout previewLayout;
    private ImageView previewPointer;
    private LinearLayout previewMain;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private Utils mUtils;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pointer_preview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.preview_fab_apply);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSureDialog();
                }
            });
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        mUtils = new Utils();

        previewLayout = (RelativeLayout) findViewById(R.id.preview_layout);
        previewPointer = (ImageView) findViewById(R.id.preview_pointer);
        previewMain = (LinearLayout) findViewById(R.id.previewMain);
        if (previewMain != null) {
            previewMain.setVisibility(View.INVISIBLE);
        }

        previewLayout.setBackgroundColor(getOldColor());

        String pointerpreviewPath = getFilesDir().getPath()+"/pointerPreview.png";
        Drawable d = Drawable.createFromPath(pointerpreviewPath);
        previewPointer.setImageDrawable(d);
        int pointersize = mSharedPreferences.getInt("POINTER_SIZE", 49);

        previewPointer.setLayoutParams(new LinearLayout.LayoutParams(pointersize, pointersize));

        previewLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action){
                    case MotionEvent.ACTION_MOVE:
                        previewMain.setVisibility(View.VISIBLE);
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        previewMain.setPadding(x, y, 0, 0);
                        break;
                }
                return true;
            }
        });

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    public void changePreviewBack(View view) {
        new ColorChooserDialog.Builder(this, R.string.choose_back_color)
                .titleSub(R.string.choose_back_color)
                .accentMode(false)
                .allowUserColorInputAlpha(true)
                .dynamicButtonColor(false)
                .preselect(getOldColor())
                .show();
    }

    private int getOldColor(){
        return mSharedPreferences.getInt("PREVIEW_OLD_COLOR", 16777215);
    }

    private int getOldPointerColor(){
        return mSharedPreferences.getInt("PREVIEW_POINTER_OLD_COLOR", -1);
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        if (dialog.getTitle() == R.string.choose_back_color){
            previewLayout.setBackgroundColor(selectedColor);
            mEditor.putInt("PREVIEW_OLD_COLOR", selectedColor);
            mEditor.apply();
        } else if (dialog.getTitle() == R.string.choose_pointer_color){
            previewPointer.setColorFilter(selectedColor);
            mEditor.putInt("PREVIEW_POINTER_OLD_COLOR", selectedColor);
            mEditor.apply();
        }

    }

    public void changePointerBack(View view) {
        new ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(true)
                .dynamicButtonColor(false)
                .preselect(getOldPointerColor())
                .show();
    }

    private void showSureDialog(){
        Drawable drawable = previewPointer.getDrawable();
        new MaterialDialog.Builder(this)
                .title("Are You Sure?")
                .theme(Theme.DARK)
                .content("Do you want to apply this pointer?")
                .positiveText("Yes")
                .negativeText("No")
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
                        mUtils.showSnackbar(previewLayout, "Pointer Applied ");
                    }
                }).show();
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

    private void confirm() throws IOException {
        String pointerPath = getFilesDir().getPath()+"/pointer.png";
        mEditor.putString(getString(R.string.key_pointerPath), pointerPath);
        mEditor.apply();
        Bitmap bitmap = loadBitmapFromView(previewPointer);
        File file = new File(pointerPath);
        Runtime.getRuntime().exec("chmod 666 "+pointerPath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            showRebootDialog();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showRebootDialog(){
        String textReboot = getString(R.string.reboot);
        new MaterialDialog.Builder(this)
                .title(textReboot)
                .theme(Theme.DARK)
                .content("All Changed applied. Do you want to Reboot?")
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

    public void resetColor(View view) {
        previewPointer.setColorFilter(null);
    }
}
