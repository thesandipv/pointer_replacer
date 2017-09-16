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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Helper Class
 */
class Utils {

    Utils(){

    }

    void showSnackbar(View view, String message){
        Snackbar snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackBar.show();
    }

    boolean isAppInstalled(Context context, String pName){
        try {
            context.getPackageManager().getApplicationInfo(pName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    void showSnackbar(View view, String message, String action){
        final Snackbar snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackBar.setAction(action, view1 -> snackBar.dismiss());
        snackBar.show();
    }

    int getDpi(Context context){
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    void showSnackbar(View view, String message, String action, View.OnClickListener action_listener){
        Snackbar snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackBar.setAction(action, action_listener);
        snackBar.show();
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
        private DisplayImageOptions options;

        static ArrayList<String> itemList = new ArrayList<>();

        PointerAdapter(Context context) {
            mContext = context;
            inflater = LayoutInflater.from(mContext);

            options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_image_loading)
                    .showImageForEmptyUri(R.drawable.ic_image_broken)
                    .showImageOnFail(R.drawable.ic_image_error)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .build();
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

            ImageLoader imageLoader = ImageLoader.getInstance();

            imageLoader.displayImage("file:///"+itemList.get(position), holder.imageView, options);

            return view;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
    }

    boolean isNetworkAvailable(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    void openFile(Context context, String filname, Uri downloadedFile){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(downloadedFile, getMimeType(filname));
        context.startActivity(intent);
    }

    String dlString(String url, boolean fetchLines) {
        String dS = "";
        try {
            DownloadTxtTask downloadTxtTask = new DownloadTxtTask();
            dS = downloadTxtTask.execute(url).get();
            downloadTxtTask.setFetchLines(fetchLines);
        } catch(Exception ex) {
            //
        }
        return dS;
    }

    private static class DownloadTxtTask extends AsyncTask< String, Integer, String > {
        boolean isFetchLines;

        void setFetchLines(boolean fetchLines){
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
}
