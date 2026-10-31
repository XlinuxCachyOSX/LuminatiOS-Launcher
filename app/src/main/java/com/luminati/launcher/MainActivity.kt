package com.luminati.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luminati.launcher.R
import android.app.WallpaperManager

class MainActivity : Activity() {

    private lateinit var workspaceRecycler: RecyclerView
    private lateinit var hotseatContainer: LinearLayout
    private lateinit var adapter: AppAdapter

    private val allApps = mutableListOf<ResolveInfo>()
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initImmersiveSystemUi()
        setupWallpaperBackground()
        initWorkspace()
        initHotseat()
        initGestureDetector()
        loadApps()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        refreshHotseat()
        loadApps()
    }

    // ------------------------------------------------------------------
    // System UI / Wallpaper
    // ------------------------------------------------------------------

    private fun initImmersiveSystemUi() {
        val decorView = window?.decorView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupWallpaperBackground() {
        val root = findViewById<View>(R.id.root_layout)
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaper = wallpaperManager.drawable
            if (wallpaper != null) {
                root.background = wallpaper
            }
        } catch (e: Exception) {
            // Fallback to solid color
        }
    }

    // ------------------------------------------------------------------
    // Workspace (RecyclerView)
    // ------------------------------------------------------------------

    private fun initWorkspace() {
        workspaceRecycler = findViewById(R.id.workspace)
        adapter = AppAdapter(this, ::launchApp, ::toggleFavorite)
        workspaceRecycler.layoutManager = GridLayoutManager(this, 4)
        workspaceRecycler.adapter = adapter
        workspaceRecycler.itemAnimator = DefaultItemAnimator()
        workspaceRecycler.setHasFixedSize(true)
        workspaceRecycler.setItemViewCacheSize(20)
    }

    private fun loadApps() {
        allApps.clear()
        allApps.addAll(AppCatalog.collectLaunchableApps(this))
        adapter.submitList(allApps.toList())
    }

    // ------------------------------------------------------------------
    // Hotseat
    // ------------------------------------------------------------------

    private fun initHotseat() {
        hotseatContainer = findViewById(R.id.hotseat)
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
            gravity = android.view.Gravity.CENTER
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
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
            gravity = android.view.Gravity.CENTER
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        icon.setImageDrawable(app.activityInfo?.loadIcon(packageManager))

        row.setOnClickListener { launchApp(app) }
        row.setOnLongClickListener { toggleFavorite(app); true }
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
        if (key in updated) updated.remove(key) else updated.add(key)
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

    // ------------------------------------------------------------------
    // Swipe gesture → open drawer
    // ------------------------------------------------------------------

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                if (deltaY < -100 && Math.abs(velocityY) > 100) {
                    startActivity(Intent(this@MainActivity, AllAppsActivity::class.java))
                    return true
                }
                return false
            }
        })
        findViewById<View>(R.id.root_layout).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    companion object {
        const val PREFS_NAME = "luminati_prefs"
        const val KEY_FAVORITES = "favorites"
    }
}
