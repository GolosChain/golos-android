package io.golos.golos.screens.story.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.setVectorDrawableStart

/**
 * Created by yuri on 13.03.18.
 */
class CommentListAdapter(private val ctx: Context,
                         private val items: List<CommentListAdapterItems>) : BaseAdapter() {
    enum class CommentListAdapterItems {
        FLAG_RED, FLAG_GRAY, EDIT
    }

    override fun getView(position: Int, convertView: View?, p2: ViewGroup?): View {
        val textView: TextView
        textView = if (convertView == null || convertView !is TextView) {
            LayoutInflater.from(p2!!.context).inflate(R.layout.vh_comment_popup, p2, false) as TextView
        } else convertView
        val item = items[position]
        when (item) {
            CommentListAdapterItems.FLAG_RED -> {
                textView.setVectorDrawableStart(R.drawable.ic_flag_20dp_red)
                textView.setText(R.string.to_flag)
            }
            CommentListAdapterItems.FLAG_GRAY -> {
                textView.setVectorDrawableStart(R.drawable.ic_flag_20dp_gray7d)
                textView.setText(R.string.to_flag)
            }
            CommentListAdapterItems.EDIT -> {
                textView.setVectorDrawableStart(R.drawable.ic_edit_gray_20dp)
                textView.setText(R.string.edit)
            }
        }

        return textView
    }


    override fun getItem(p0: Int): Any = items[p0]

    override fun getItemId(p0: Int) = p0.toLong()

    override fun getCount() = items.size
}