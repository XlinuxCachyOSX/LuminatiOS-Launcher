package com.luminati.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.Gravity
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.luminati.launcher.R

class MainActivity : Activity() {

    private lateinit var workspaceGrid: GridView
    private lateinit var hotseatContainer: LinearLayout

    private val allApps = mutableListOf<ResolveInfo>()
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initImmersiveSystemUi()
        initWorkspace()
        initHotseat()
        loadApps()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        refreshHotseat()
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
        workspaceGrid = findViewById(R.id.workspace)
        workspaceGrid.numColumns = 4
        workspaceGrid.stretchMode = GridView.STRETCH_COLUMN_WIDTH
        workspaceGrid.columnWidth = computeColumnWidth()
        workspaceGrid.adapter = AppGridAdapter(this, allApps)
        workspaceGrid.setOnItemClickListener { _, _, position, _ ->
            launchApp(allApps[position])
        }
        workspaceGrid.setOnItemLongClickListener { _, _, position, _ ->
            toggleFavorite(allApps[position])
            true
        }
    }

    private fun computeColumnWidth(): Int {
        val density = resources.displayMetrics.density
        val availableWidth = resources.displayMetrics.widthPixels - 16 * density
        return (availableWidth / 4).toInt()
    }

    private fun loadApps() {
        allApps.clear()
        allApps.addAll(AppCatalog.collectLaunchableApps(this))
        workspaceGrid.adapter = AppGridAdapter(this, allApps)
    }

    // ------------------------------------------------------------------
    // Hotseat
    // ------------------------------------------------------------------

    private fun initHotseat() {
        hotseatContainer = findViewById(R.id.hotseat)
        hotseatContainer.removeAllViews()
        refreshHotseat()
    }

    private fun refreshHotseat() {
        hotseatContainer.removeAllViews()
        hotseatContainer.addView(createAllAppsIcon())
        for (key in favorites) {
            val app = findApp(key)
            if (app != null) {
                val row = createHotseatIcon(app)
                hotseatContainer.addView(row)
                row.alpha = 0f
                row.animate().alpha(1f).setDuration(200).start()
            }
        }
    }

    private fun createAllAppsIcon(): LinearLayout {
        val density = resources.displayMetrics.density
        val iconSizePx = (48 * density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt()
            )
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        icon.setImageDrawable(getDrawable(R.drawable.ic_all_apps))

        row.setOnClickListener { startActivity(Intent(this, AllAppsActivity::class.java)) }
        row.addView(icon)

        return row
    }

    private fun createHotseatIcon(app: ResolveInfo): LinearLayout {
        val density = resources.displayMetrics.density
        val iconSizePx = (48 * density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt()
            )
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        icon.setImageDrawable(app.activityInfo?.loadIcon(packageManager))

        row.setOnClickListener { launchApp(app) }
        row.addView(icon)

        return row
    }

    // ------------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------------

    private fun loadFavorites() {
        favorites.clear()
        favorites.addAll(prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty())
        refreshHotseat()
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val key = appKey(app)
        if (key.isEmpty()) return
        toggleFavorite(key)
    }

    private fun toggleFavorite(key: String) {
        val updated = favorites.toMutableSet()
        if (key in updated) {
            updated.remove(key)
        } else {
            updated.add(key)
        }
        favorites.clear()
        favorites.addAll(updated)
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
        refreshHotseat()
    }

    private fun appKey(app: ResolveInfo): String {
        val info = app.activityInfo ?: return ""
        return ComponentName(info.packageName, info.name).flattenToString()
    }

    private fun findApp(key: String): ResolveInfo? = allApps.firstOrNull { appKey(it) == key }

    // ------------------------------------------------------------------
    // Launch
    // ------------------------------------------------------------------

    private fun launchApp(app: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app.activityInfo?.packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // App non più installabile
        }
    }

    private fun launchComponent(key: String) {
        val parts = key.split("/")
        if (parts.size < 2) return
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(parts[0], parts[1]))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // App non più installabile
        }
    }

    companion object {
        const val PREFS_NAME = "luminati_prefs"
        const val KEY_FAVORITES = "favorites"
    }
}
