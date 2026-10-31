package com.luminati.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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
    private lateinit var clockText: TextView
    private lateinit var searchText: TextView

    private val allApps = mutableListOf<ResolveInfo>()
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            initImmersiveSystemUi()
            setupWallpaperBackground()
            initWorkspace()
            initHotseat()
            initClock()
            initSearch()
            initGestureDetector()
            loadApps()
            loadFavorites()
        } catch (e: Exception) {
            // Graceful degradation
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            refreshHotseat()
            loadApps()
            updateClock()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
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
                // Add scrim for readability
                root.foreground = android.graphics.drawable.ColorDrawable(Color.argb(80, 0, 0, 0))
            }
        } catch (e: Exception) {
            root.setBackgroundColor(Color.parseColor("#1a1a2e"))
        }
    }

    // ------------------------------------------------------------------
    // Clock
    // ------------------------------------------------------------------

    private fun initClock() {
        clockText = findViewById(R.id.clock_text)
        updateClock()
        clockHandler.postDelayed(object : Runnable {
            override fun run() {
                updateClock()
                clockHandler.postDelayed(this, 60000)
            }
        }, 60000)
    }

    private fun updateClock() {
        try {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault())
            clockText.text = String.format("%02d:%02d", hour, minute)
            findViewById<TextView>(R.id.date_text)?.text = String.format("%d %s", day, month ?: "")
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    private fun initSearch() {
        searchText = findViewById(R.id.search_text)
        searchText.setOnClickListener {
            startActivity(Intent(this, AllAppsActivity::class.java))
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
        try {
            allApps.clear()
            allApps.addAll(AppCatalog.collectLaunchableApps(this))
            adapter.submitList(allApps.toList())
        } catch (e: Exception) {
            // Show empty state
        }
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

    private fun createHotseatIcon(app: ResolveInfo): LinearLayout {
        val density = resources.displayMetrics.density
        val iconSizePx = (44 * density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        try {
            icon.setImageDrawable(app.activityInfo?.loadIcon(packageManager))
        } catch (e: Exception) {
            // Ignore
        }

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
        try {
            favorites.addAll(prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty())
        } catch (e: Exception) {
            // Ignore
        }
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
