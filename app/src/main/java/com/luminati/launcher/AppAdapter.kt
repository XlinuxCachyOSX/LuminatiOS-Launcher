package com.luminati.launcher

import android.content.Context
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val context: Context,
    private val onAppClick: (ResolveInfo) -> Unit = {},
    private val onAppLongClick: (ResolveInfo) -> Unit = {}
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private val items = mutableListOf<ResolveInfo>()
    private val iconCache = LruCache<String, Drawable>(64)

    init {
        setHasStableIds(true)
    }

    fun submitList(newItems: List<ResolveInfo>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                itemKey(items[oldItemPosition]) == itemKey(newItems[newItemPosition])
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                items[oldItemPosition].activityInfo?.enabled == newItems[newItemPosition].activityInfo?.enabled
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemId(position: Int): Long = itemKey(items[position]).hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_shortcut, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        val info = app.activityInfo
        val pm = context.packageManager

        holder.label.text = info?.loadLabel(pm).toString()

        val key = itemKey(app)
        val cached = iconCache.get(key)
        if (cached != null) {
            holder.icon.setImageDrawable(cached)
        } else {
            val drawable = info?.loadIcon(pm)
            if (drawable != null) iconCache.put(key, drawable)
            holder.icon.setImageDrawable(drawable)
        }

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { onAppLongClick(app); true }
    }

    override fun getItemCount(): Int = items.size

    private fun itemKey(app: ResolveInfo): String {
        val info = app.activityInfo
        return "${info?.packageName}/${info?.name}"
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.shortcut_icon)
        val label: TextView = itemView.findViewById(R.id.shortcut_label)
    }
}
