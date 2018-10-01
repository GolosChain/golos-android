package io.golos.golos.screens.tags.adapters

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import io.golos.golos.R

/**
 * Created by yuri on 09.01.18.
 */
class StartMarginDecorator : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
        outRect.set(parent?.context?.resources?.getDimension(R.dimen.margin_material_half)?.toInt() ?: 0, 0, 0, 0)
    }
}