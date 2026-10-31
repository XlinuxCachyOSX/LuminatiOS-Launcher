package com.luminati.launcher

import android.content.Context
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AppGridAdapter(
    private val context: Context,
    private val apps: List<ResolveInfo>,
    private val onAppClick: (ResolveInfo) -> Unit = {},
    private val onAppLongClick: (ResolveInfo) -> Unit = {}
) : BaseAdapter() {

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): ResolveInfo = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = if (convertView != null) {
            convertView
        } else {
            LayoutInflater.from(context).inflate(R.layout.item_shortcut, parent, false).also { item ->
                item.alpha = 0f
                item.scaleX = 0.85f
                item.scaleY = 0.85f
                item.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
            }
        }

        val icon = view.findViewById<ImageView>(R.id.shortcut_icon)
        val label = view.findViewById<TextView>(R.id.shortcut_label)
        val activityInfo = apps[position].activityInfo

        icon.setImageDrawable(activityInfo?.loadIcon(context.packageManager))
        label.text = activityInfo?.loadLabel(context.packageManager).toString()

        return view
    }
}
