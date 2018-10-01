package io.golos.golos.screens.settings

import android.content.Context
import androidx.annotation.ArrayRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.inflate

/**
 * Created by yuri on 06.02.18.
 */
class SettingsSpinnerAdapter(context: Context, @ArrayRes stringArrayId: Int) : ArrayAdapter<String>(context,
        android.R.layout.simple_list_item_1,
        context.resources.getStringArray(stringArrayId)) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = parent!!.inflate<ViewGroup>(R.layout.vh_spinner_daynight)
        v.findViewById<TextView>(R.id.text1).text = getItem(position)
        return v
    }
}
