package io.golos.golos.screens.widgets

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by yuri on 30.10.17.
 */
abstract class GolosViewHolder(@LayoutRes res: Int, parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(inflate(res, parent)) {

    companion object {
        fun inflate(@LayoutRes res: Int, parent: ViewGroup): View {
            return LayoutInflater.from(parent.context).inflate(res, parent, false)
        }
    }
}


