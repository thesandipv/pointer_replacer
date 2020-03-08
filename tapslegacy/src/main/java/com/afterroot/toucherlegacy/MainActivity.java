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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class MainActivity extends Activity {
    static String EXTRA_TOUCH_VAL = "com.afterroot.toucherlegacy.EXTRA_TOUCH_VAL";
    String _tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setFinishOnTouchOutside(false);

        try {
            showTouches(getIntent().getExtras().getInt(EXTRA_TOUCH_VAL, 0));
        } catch (NullPointerException ignored) {

        }
    }

    void showTouches(int value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Log.d(_tag, "Write Settings permission not granted");
                setResult(2);
                finish();
            } else {
                try {
                    Settings.System.putInt(getContentResolver(), getString(R.string.key_show_touches), value);
                } catch (Exception e) {
                    Log.e(_tag, e.toString());
                    setResult(3);
                    finish();
                } finally {
                    setResult(1);
                    finish();
                }
            }
        }
    }
}
