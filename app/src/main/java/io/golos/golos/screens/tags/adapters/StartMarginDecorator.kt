package io.golos.golos.screens.tags.adapters

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import io.golos.golos.R

/**
 * Created by yuri on 09.01.18.
 */
class StartMarginDecorator : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        outRect?.set(parent?.context?.resources?.getDimension(R.dimen.margin_material_half)?.toInt() ?: 0, 0, 0, 0)
    }
}