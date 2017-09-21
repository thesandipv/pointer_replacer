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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Helper Class
 */
class Utils {

    Utils(){

    }

    boolean isAppInstalled(Context context, String pName){
        try {
            context.getPackageManager().getApplicationInfo(pName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    void showSnackBar(View view, String message){
        this.showSnackBar(view, message, null);
    }

    void showSnackBar(View view, String message, String action){
        this.showSnackBar(view, message, action, view1 -> {});
    }

    void showSnackBar(View view, String message, String action, View.OnClickListener action_listener){
        this.showSnackBar(view, message, Snackbar.LENGTH_LONG, action, action_listener);
    }

    void showSnackBar(View view, String message, int length, String action, View.OnClickListener action_listener){
        Snackbar snackBar = Snackbar.make(view, message, length);
        snackBar.setAction(action, action_listener);
        snackBar.show();
    }

    int getDpi(Context context){
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    /**
     * @param fileName name of file
     * @return extension of fileName
     */
    @NonNull
    private String getFileExt(String fileName) {
        return fileName.substring((fileName.lastIndexOf(".")+ 1 ), fileName.length());
    }

    /**
     * @param fileName name of file
     * @return mime type of fileName
     */
    private String getMimeType(String fileName) {
        String type = null;
        try {
            String extension = getFileExt(fileName);
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        } catch (Exception e){
            e.printStackTrace();
        }
        return type;
    }

    void copyFile(File source, File destination) throws IOException {
        FileUtils.copyFile(source, destination);
    }

    void loadToBottomSheetGrid(Context context,
                                      GridView target,
                                      String targetPath,
                                      AdapterView.OnItemClickListener listener){
        final PointerAdapter pointerAdapter = new PointerAdapter(context);
        if (getDpi(context) <= 240){
            pointerAdapter.setLayoutParams(49);
        } else if (getDpi(context) >= 240){
            pointerAdapter.setLayoutParams(66);
        }

        target.setAdapter(pointerAdapter);
        target.setOnItemClickListener(listener);

        File[] files = new File(targetPath).listFiles();
        try {
            for (File file : files){
                pointerAdapter.add(file.getAbsolutePath());
            }
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    /**GridView Image Adapter.**/
    static class PointerAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater inflater;

        static ArrayList<String> itemList = new ArrayList<>();

        PointerAdapter(Context context) {
            mContext = context;
            inflater = LayoutInflater.from(mContext);
        }
        void add(String path) {
            itemList.add(path);
        }

        void clear(){
            itemList.clear();
        }

        String getPath(int index){
            return itemList.get(index);
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int arg0){
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        int param = 49;
        void setLayoutParams(int i){
            param = i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;

            if (view == null){
                view = inflater.inflate(R.layout.gridview_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = view.findViewById(R.id.grid_item_image);
                holder.imageView.setLayoutParams(new FrameLayout.LayoutParams(param, param));
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Glide
                    .with(mContext)
                    .load(itemList.get(position))
                    .into(holder.imageView);

            return view;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
    }

    boolean isNetworkAvailable(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    void openFile(Context context, String filename, Uri uri){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getMimeType(filename));
        context.startActivity(intent);
    }
}
