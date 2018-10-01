package io.golos.golos.screens.profile.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.profile.KeyValueRow
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.setVectorDrawableEnd

data class KeyValueRowWrapper(val keyValueRow: KeyValueRow,
                              val onClickListener: (KeyValueRow) -> Unit)

class KeyValueAdapter(initialValues: List<KeyValueRow>,
                      val clickListener: (KeyValueRow) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<KeyValueViewHolder>() {

    var values = initialValues
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        field[oldItemPosition] == value[newItemPosition]

                override fun getOldListSize(): Int = field.size

                override fun getNewListSize(): Int = value.size

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        field[oldItemPosition] == value[newItemPosition]

            }).dispatchUpdatesTo(this)
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyValueViewHolder =
            KeyValueViewHolder(parent)

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: KeyValueViewHolder, position: Int) {
        holder.state = KeyValueRowWrapper(values[position], { clickListener.invoke(it) })
    }
}


class KeyValueViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_key_value, parent) {
    private val mKeyTv = itemView.findViewById<TextView>(R.id.key_tv)
    private val mValueTv = itemView.findViewById<TextView>(R.id.value_tv)

    var state: KeyValueRowWrapper = KeyValueRowWrapper(KeyValueRow("", ""), {})
        set(value) {
            field = value
            mKeyTv.text = field.keyValueRow.key
            mValueTv.text = field.keyValueRow.value
            value.keyValueRow.valueEndDrawable?.let {
                mValueTv.setVectorDrawableEnd(it)
            }
            itemView.setOnClickListener { value.onClickListener.invoke(field.keyValueRow) }
        }
}