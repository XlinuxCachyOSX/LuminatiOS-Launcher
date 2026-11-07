package com.luminati.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var dateText: TextView
    private lateinit var searchText: TextView
    private lateinit var rootLayout: View

    private val allApps = mutableListOf<ResolveInfo>()
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    companion object {
        const val REQUEST_HOME_ROLE = 1001
        const val PREFS_NAME = "luminati_prefs"
        const val KEY_FAVORITES = "favorites"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            initImmersiveSystemUi()
            setupWallpaperBackground()
            initWorkspace()
            initHotseat()
            initClock()
            initSearch()
            initGestureDetector()
            loadApps()
            loadFavorites()
            requestDefaultLauncherRole()
        } catch (e: Exception) {
            try {
                setContentView(R.layout.activity_main)
                initWorkspace()
                loadApps()
            } catch (e2: Exception) {
                finish()
            }
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
    // Default Launcher Role
    // ------------------------------------------------------------------

    private fun requestDefaultLauncherRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivityForResult(intent, REQUEST_HOME_ROLE)
                }
            }
        } catch (e: Exception) {
            // Ignore - role request may not be available
        }
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
        rootLayout = findViewById(R.id.root_layout)
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaper = wallpaperManager.drawable
            if (wallpaper != null) {
                rootLayout.background = wallpaper
                rootLayout.foreground = android.graphics.drawable.ColorDrawable(Color.argb(100, 0, 0, 0))
            } else {
                rootLayout.setBackgroundColor(Color.parseColor("#0d0d1a"))
            }
        } catch (e: Exception) {
            rootLayout.setBackgroundColor(Color.parseColor("#0d0d1a"))
        }
    }

    // ------------------------------------------------------------------
    // Clock
    // ------------------------------------------------------------------

    private fun initClock() {
        try {
            clockText = findViewById(R.id.clock_text)
            dateText = findViewById(R.id.date_text)
            updateClock()
            clockHandler.postDelayed(object : Runnable {
                override fun run() {
                    updateClock()
                    clockHandler.postDelayed(this, 60000)
                }
            }, 60000)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun updateClock() {
        try {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault())
            clockText.text = String.format("%02d:%02d", hour, minute)
            dateText.text = String.format("%d %s", day, month ?: "")
        } catch (e: Exception) {
            clockText.text = "--:--"
        }
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    private fun initSearch() {
        searchText = findViewById(R.id.search_text)
        searchText.setOnClickListener {
            try {
                startActivity(Intent(this, AllAppsActivity::class.java))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // ------------------------------------------------------------------
    // Workspace (RecyclerView)
    // ------------------------------------------------------------------

    private fun initWorkspace() {
        workspaceRecycler = findViewById(R.id.workspace)
        adapter = AppAdapter(this, ::launchApp, ::showAppContextMenu)
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
            val emptyState = findViewById<TextView>(R.id.empty_state)
            if (allApps.isEmpty()) {
                workspaceRecycler.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                workspaceRecycler.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
            }
        } catch (e: Exception) {
            // Show empty state
            try {
                workspaceRecycler.visibility = View.GONE
                findViewById<TextView>(R.id.empty_state).visibility = View.VISIBLE
            } catch (e2: Exception) {
                // Ignore
            }
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
        if (favorites.isEmpty()) {
            // Show default apps in hotseat
            val defaultApps = allApps.take(4)
            for (app in defaultApps) {
                val row = createHotseatIcon(app)
                hotseatContainer.addView(row)
                row.alpha = 0f
                row.animate().alpha(1f).setDuration(200).start()
            }
        } else {
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
    }

    private fun createHotseatIcon(app: ResolveInfo): LinearLayout {
        val density = resources.displayMetrics.density
        val iconSizePx = (44 * density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
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
        row.setOnLongClickListener {
            try {
                val popup = PopupMenu(this, row)
                val label = app.activityInfo?.loadLabel(packageManager)?.toString() ?: "App"
                popup.menu.add(0, 1, 0, "Apri $label")
                popup.menu.add(0, 2, 1, "Info app")
                popup.menu.add(0, 3, 2, "Disinstalla")
                val key = appKey(app)
                if (key in favorites) {
                    popup.menu.add(0, 4, 3, "Rimuovi dai preferiti")
                } else {
                    popup.menu.add(0, 4, 3, "Aggiungi ai preferiti")
                }
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> launchApp(app)
                        2 -> openAppInfo(app)
                        3 -> uninstallApp(app)
                        4 -> toggleFavorite(app)
                    }
                    true
                }
                popup.show()
            } catch (e: Exception) {
                // Ignore
            }
            true
        }
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
    // Context Menu Actions
    // ------------------------------------------------------------------

    private fun showAppContextMenu(app: ResolveInfo) {
        try {
            val anchor = findViewById<View>(R.id.workspace) ?: return
            val popup = PopupMenu(this, anchor)
            val label = app.activityInfo?.loadLabel(packageManager)?.toString() ?: "App"
            popup.menu.add(0, 1, 0, "Apri $label")
            popup.menu.add(0, 2, 1, "Info app")
            popup.menu.add(0, 3, 2, "Disinstalla")
            val key = appKey(app)
            if (key in favorites) {
                popup.menu.add(0, 4, 3, "Rimuovi dai preferiti")
            } else {
                popup.menu.add(0, 4, 3, "Aggiungi ai preferiti")
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> launchApp(app)
                    2 -> openAppInfo(app)
                    3 -> uninstallApp(app)
                    4 -> toggleFavorite(app)
                }
                true
            }
            popup.show()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun openAppInfo(app: ResolveInfo) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${app.activityInfo?.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun uninstallApp(app: ResolveInfo) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${app.activityInfo?.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            // Ignore
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

    // End of class
}
