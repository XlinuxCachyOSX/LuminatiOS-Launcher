package com.luminati.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Riceve le richieste di conferma dei PIN (app shortcut e widget) provenienti
 * dal sistema, in modo che il launcher possa gestire l'operazione di pinning.
 */
class PinConfirmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // In una implementazione completa: mostrare la dialog di conferma e,
        // al confermare, richiamare LauncherApps.startShortcut(...)/startAppDetailsActivity(...).
    }
}
