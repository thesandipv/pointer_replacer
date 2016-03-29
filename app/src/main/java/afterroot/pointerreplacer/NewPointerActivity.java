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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.android.graphics.CanvasView;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static afterroot.pointerreplacer.Utils.getPointerFolderPath;
import static afterroot.pointerreplacer.Utils.loadToBottomSheetGrid;

public class NewPointerActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback {
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;
    BottomSheetLayout mBottomSheetLayout;
    FloatingActionButton fab;
    CanvasView mCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_pointer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyNewPointer();
                //fab.hide();
            }
        });
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();
        
        setBottomSheetLayout();

        setCanvasView();

    }

    public void setCanvasView(){
        mCanvas = (CanvasView) findViewById(R.id.canvasView);
        mCanvas.setMode(CanvasView.Mode.DRAW);
        mCanvas.setDrawer(CanvasView.Drawer.CIRCLE);
        mCanvas.setBaseColor(Color.WHITE);
        mCanvas.setPaintStyle(Paint.Style.FILL);
        mCanvas.setPaintStrokeColor(mSharedPreferences.getInt("stroke_color", Color.BLACK));
        mCanvas.setPaintStrokeWidth(2F);
    }

    public void setBottomSheetLayout(){
        mBottomSheetLayout = (BottomSheetLayout) findViewById(R.id.BS_newPointer);
        mBottomSheetLayout.addOnSheetDismissedListener(new OnSheetDismissedListener() {
            @Override
            public void onDismissed(BottomSheetLayout bottomSheetLayout) {
                fab.show();
            }
        });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.drawing_brush:
                mCanvas.setMode(CanvasView.Mode.DRAW);
                mCanvas.setDrawer(CanvasView.Drawer.PEN);
                mCanvas.setPaintStyle(Paint.Style.STROKE);
            case R.id.drawing_eraser:
                mCanvas.setMode(CanvasView.Mode.ERASER);
                break;
            case R.id.drawing_text:
                mCanvas.setMode(CanvasView.Mode.TEXT);
                new MaterialDialog.Builder(NewPointerActivity.this)
                        .title("Set Custom Text")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("Custom Text", null, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                mCanvas.setText(input.toString());
                            }
                        }).show();
                break;
            case R.id.choose_pointer:
                showBottom();
                break;
        }
    }

    public void setCanvasLayoutParams(int width, int height){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        mCanvas.setLayoutParams(params);

    }

    public void showBottom(){
        fab.hide();
        mBottomSheetLayout.addOnSheetDismissedListener(new OnSheetDismissedListener() {
            @Override
            public void onDismissed(BottomSheetLayout bottomSheetLayout) {
                fab.show();
            }
        });
        mBottomSheetLayout.showWithSheetView(LayoutInflater
                .from(getApplicationContext())
                .inflate(R.layout.gridview_bottomsheet, mBottomSheetLayout, false));
        GridView gridView = (GridView) findViewById(R.id.bs_gridView);
        loadToBottomSheetGrid(getApplicationContext(), gridView, getPointerFolderPath(getApplicationContext()), new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bitmap bitmap = BitmapFactory.decodeFile(Utils.PointerAdapter.getPath(i));
                mCanvas.drawBitmap(BitmapFactory.decodeFile(Utils.PointerAdapter.getPath(i)));
                setCanvasLayoutParams(bitmap.getWidth(), bitmap.getHeight());
                mBottomSheetLayout.dismissSheet();
            }
        });
    }


    public void confirm() throws IOException {
        String pointerPath = getFilesDir().getPath()+"/pointer.png";
        mEditor.putString(getString(R.string.key_pointerPath), pointerPath);
        mEditor.apply();
        Bitmap bitmap = mCanvas.getBitmap();
        File file = new File(pointerPath);
        Runtime.getRuntime().exec("chmod 666 "+pointerPath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void applyNewPointer() {
        try {
            confirm();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        mEditor.putInt("stroke_color", selectedColor).apply();
        mCanvas.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_pointer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.undo:
                mCanvas.undo();
                break;
            case R.id.redo:
                mCanvas.redo();
                break;
            case R.id.clear_canvas:
                mCanvas.clear();
                break;
        }
        return true;
    }
}
