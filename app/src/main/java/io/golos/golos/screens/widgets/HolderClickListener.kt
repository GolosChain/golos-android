package io.golos.golos.screens.widgets

import android.support.v7.widget.RecyclerView

/**
 * Created by yuri on 20.02.18.
 */
interface HolderClickListener {
    fun onClick(holder: RecyclerView.ViewHolder)
}