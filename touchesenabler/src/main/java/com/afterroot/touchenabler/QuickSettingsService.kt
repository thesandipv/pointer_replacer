/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.touchenabler

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi


@RequiresApi(api = Build.VERSION_CODES.N)
class QuickSettingsService : TileService() {

    override fun onTileAdded() {
    }

    override fun onStartListening() {
    }

    override fun onClick() {
        if (!getShowTouches()) {
            qsTile.state = Tile.STATE_ACTIVE
        } else {
            qsTile.state = Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
        val i = Intent().apply {
            action = MainFragment.ACTION_OPEN_TEL
            putExtra("com.afterroot.toucherlegacy.EXTRA_TOUCH_VAL", if (!getShowTouches()) 1 else 0)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (i.resolveActivity(applicationContext.packageManager) != null) {
            startActivity(i)
        } else {
            Toast.makeText(this, getString(R.string.msg_install_extension_first), Toast.LENGTH_SHORT).show()
            startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun getShowTouches(): Boolean {
        return Settings.System.getInt(applicationContext.contentResolver, getString(R.string.key_show_touches)) == 1
    }

}
