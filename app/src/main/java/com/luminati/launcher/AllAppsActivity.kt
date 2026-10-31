package com.luminati.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luminati.launcher.R

class AllAppsActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText
    private lateinit var adapter: AppAdapter

    private val allApps = mutableListOf<ResolveInfo>()
    private val prefs by lazy { getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        initImmersiveSystemUi()
        initViews()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun initImmersiveSystemUi() {
        val decorView = window?.decorView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.all_apps)
        searchView = findViewById(R.id.search)

        adapter = AppAdapter(this, ::launchApp, ::toggleFavorite)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)

        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener { finish() }
    }

    private fun loadApps() {
        allApps.clear()
        allApps.addAll(AppCatalog.collectLaunchableApps(this))
        filterApps(searchView.text.toString())
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allApps.toList()
        } else {
            allApps.filter { app ->
                app.activityInfo?.loadLabel(packageManager)?.toString()?.lowercase()?.contains(q) == true
            }
        }
        adapter.submitList(filtered)
    }

    private fun launchApp(app: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app.activityInfo?.packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // App non più installabile
        }
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val info = app.activityInfo ?: return
        val key = ComponentName(info.packageName, info.name).flattenToString()
        val updated = prefs.getStringSet(MainActivity.KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()
        if (key in updated) updated.remove(key) else updated.add(key)
        prefs.edit().putStringSet(MainActivity.KEY_FAVORITES, updated).apply()
    }
}
