package com.luminati.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

/**
 * Catalogo di tutte le applicazioni installate che dichiarano
 * l'azione MAIN con la categoria LAUNCHER (quindi quelle che possono
 * essere avviate dalla home screen).
 */
object AppCatalog {

    fun collectLaunchableApps(context: Context): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentActivities(intent, 0)
        return resolveInfos
            .filter { resolveInfo -> resolveInfo.activityInfo?.enabled ?: true }
            .sortedBy { resolveInfo ->
                resolveInfo.activityInfo?.loadLabel(context.packageManager)?.toString().orEmpty()
                    .lowercase()
            }
    }
}
