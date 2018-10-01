package io.golos.golos.screens.widgets

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by yuri on 20.02.18.
 */
interface HolderClickListener {
    fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder)
}