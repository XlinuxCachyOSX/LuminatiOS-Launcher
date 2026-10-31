package com.luminati.launcher

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.luminati.launcher.R

/**
 * Quick Settings tile personalizzato di LuminatiOS.
 * Si attiva quando il sistema avvia l'ascolto del tile (quick settings).
 */
class LuminatiOSTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    }

    override fun onStartListening() {
        val tile = getQsTile() ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_all_apps)
        tile.updateTile()
    }

    override fun onStopListening() {
        val tile = getQsTile() ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
