package io.golos.golos.screens.profile.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import io.golos.golos.R

/**
 * Created by yuri on 10.11.17.
 */
data class MenuItem(val text: String,
                    val startIcon: Int,
                    val id: Int)

class MenuAdapter(private val items: List<MenuItem>,
                  private val onItemClick: ((MenuItem)->Unit),
                  context: Context) : ArrayAdapter<MenuItem>(context, R.layout.lv_menu_item) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var v = convertView
        if (v == null) {
            v = LayoutInflater.from(parent!!.context).inflate(R.layout.lv_menu_item, parent, false)
        }
        val text = v!!.findViewById<TextView>(R.id.text)
        val image = v.findViewById<ImageView>(R.id.image)
        text.text = items[position].text
        image.setImageResource(items[position].startIcon)
        text.setOnClickListener({ onItemClick.invoke(items[position]) })
        return v
    }

    override fun getCount(): Int {
        return items.count()
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }
}