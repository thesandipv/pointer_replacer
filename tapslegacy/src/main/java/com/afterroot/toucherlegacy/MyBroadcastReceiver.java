/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.toucherlegacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public String TAG = "MyBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Received");
        Toast.makeText(context, "Something Removed", Toast.LENGTH_SHORT).show();
        if (intent != null) {
            try {
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
                    Log.d(TAG, "onReceive: Raw " + intent.getData());
                    Log.d(TAG, "onReceive: host " + intent.getData().getHost());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
}
