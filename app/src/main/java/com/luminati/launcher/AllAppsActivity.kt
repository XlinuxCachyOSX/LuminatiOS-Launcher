package com.luminati.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.widget.GridView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.luminati.launcher.R

class AllAppsActivity : Activity() {

    private lateinit var gridView: GridView

    private val allApps = mutableListOf<ResolveInfo>()
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        initImmersiveSystemUi()
        initWorkspace()
        loadApps()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    // ------------------------------------------------------------------
    // System UI
    // ------------------------------------------------------------------

    private fun initImmersiveSystemUi() {
        val decorView = window?.decorView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    // ------------------------------------------------------------------
    // Workspace
    // ------------------------------------------------------------------

    private fun initWorkspace() {
        gridView = findViewById(R.id.all_apps)
        gridView.numColumns = 4
        gridView.stretchMode = GridView.STRETCH_COLUMN_WIDTH
        gridView.columnWidth = computeColumnWidth()
    }

    private fun computeColumnWidth(): Int {
        val density = resources.displayMetrics.density
        val availableWidth = resources.displayMetrics.widthPixels - 16 * density
        return (availableWidth / 4).toInt()
    }

    private fun loadApps() {
        allApps.clear()
        allApps.addAll(AppCatalog.collectLaunchableApps(this))
        gridView.adapter = AppGridAdapter(this, allApps, ::launchApp)
        gridView.setOnItemClickListener { _, _, position, _ ->
            launchApp(allApps[position])
        }
        gridView.setOnItemLongClickListener { _, _, position, _ ->
            toggleFavorite(allApps[position])
            true
        }
    }

    // ------------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------------

    private fun loadFavorites() {
        favorites.clear()
        favorites.addAll(prefs.getStringSet(MainActivity.KEY_FAVORITES, emptySet()).orEmpty())
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val info = app.activityInfo ?: return
        val key = ComponentName(info.packageName, info.name).flattenToString()
        val updated = favorites.toMutableSet()
        if (key in updated) {
            updated.remove(key)
        } else {
            updated.add(key)
        }
        favorites.clear()
        favorites.addAll(updated)
        prefs.edit().putStringSet(MainActivity.KEY_FAVORITES, updated).apply()
    }

    // ------------------------------------------------------------------
    // Launch
    // ------------------------------------------------------------------

    private fun launchApp(app: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN, null)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(app.activityInfo?.packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // App non più installabile
        }
    }
}
