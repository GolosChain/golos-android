package io.golos.golos.screens.settings

import android.graphics.PorterDuff
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DiffUtil
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.getColorCompat
import io.golos.golos.utils.toArrayList
import java.util.*

class SettingsAdapter(list: List<SettingRow>,
                      var onSwitch: (id: Any, oldValue: Boolean, newValue: Boolean) -> Unit = { _, _, _ -> }) :
        androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    var rows: List<SettingRow> = list
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        field[oldItemPosition].id == value[newItemPosition].id


                override fun getOldListSize() = field.size


                override fun getNewListSize() = value.size

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        field[oldItemPosition] == value[newItemPosition]

            }).dispatchUpdatesTo(this)
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.vh_key_title -> TitleViewHolder(parent)
            R.layout.vh_key_delimeter -> DelimeterRowHolder(parent)
            R.layout.vh_key_switch -> SwitchRowHolder(parent)
            R.layout.vh_space -> SpaceRowHolder(parent)
            R.layout.vh_settings_check -> CheckRowHolder(parent)
            else -> DelimeterRowHolder(parent)
        }
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        val item = rows[position]
        when (item) {
            is TitleRow -> (holder as? TitleViewHolder)?.state = item
            is SwitchRow -> (holder as?SwitchRowHolder)?.state = SwitchRowHolder.SwitchRowWrapper(item, onSwitch)
            is SpaceRow -> (holder as?SpaceRowHolder)?.state = item
            is CheckRow -> (holder as? CheckRowHolder)?.state = CheckRowHolder.CheckRowWrapper(item, onSwitch)
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is TitleRow -> R.layout.vh_key_title
            is DelimeterRow -> R.layout.vh_key_delimeter
            is SwitchRow -> R.layout.vh_key_switch
            is SpaceRow -> R.layout.vh_space
            is CheckRow -> R.layout.vh_settings_check
        }
    }

    class TitleViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_key_title, parent) {
        private val title = itemView.findViewById<TextView>(R.id.title_tv)

        var state: TitleRow = TitleRow(UUID.randomUUID().toString(), 0)
            set(value) {
                if (value != field) {
                    title.setText(value.titleId)
                    field = value
                }
            }
    }

    class SpaceRowHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_space, parent) {
        var state: SpaceRow? = null
            set(value) {
                field = value
                if (value != null) itemView.minimumHeight = itemView.resources.getDimension(value.space).toInt()
            }
    }

    class DelimeterRowHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_key_delimeter, parent)

    class CheckRowHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_settings_check, parent) {
        private val mCheckBox = itemView.findViewById<AppCompatCheckBox>(R.id.checkbox_cb)
        private val mImageView = itemView.findViewById<AppCompatImageView>(R.id.image_view)
        private var mLastDrawableId = 0

        data class CheckRowWrapper(val checkRow: CheckRow, val clickListener: (id: Any, oldValue: Boolean, newValue: Boolean) -> Unit)

        init {
            mImageView.setOnClickListener { mCheckBox.callOnClick() }
        }

        var state: CheckRowWrapper? = null
            set(value) {
                if (value == null) return

                val checkRow = value.checkRow
                if (checkRow.isChecked != mCheckBox.isChecked) mCheckBox.isChecked = checkRow.isChecked
                if (mLastDrawableId != checkRow.imageReId) mImageView.setImageResource(checkRow.imageReId)
                mCheckBox.setText(checkRow.textId)

                if (checkRow.isChecked) {
                    mImageView.setColorFilter(itemView.getColorCompat(R.color.settings_checked_tint), PorterDuff.Mode.SRC_ATOP)
                } else {
                    mImageView.setColorFilter(itemView.getColorCompat(R.color.settings_unchecked_tint), PorterDuff.Mode.SRC_ATOP)
                }
                mCheckBox.setOnClickListener {
                    state = this.state?.copy(checkRow = this.state?.checkRow?.switch()
                            ?: return@setOnClickListener)
                    value.clickListener.invoke(value.checkRow.id, field?.checkRow?.isChecked == true, mCheckBox.isChecked)
                }
                field = value
                mLastDrawableId = checkRow.imageReId
            }
    }

    class SwitchRowHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_key_switch, parent) {
        private val mTextView: TextView = itemView.findViewById(R.id.key_tv)
        private val mSwitch: SwitchCompat = itemView.findViewById(R.id.value_switch)


        data class SwitchRowWrapper(val switchRow: SwitchRow, val clickListener: (id: Any, oldValue: Boolean, newValue: Boolean) -> Unit)


        var state: SwitchRowWrapper? = null
            set(value) {
                if (value == null) {
                    field = value
                } else {
                    mTextView.setText(value.switchRow.textId)
                    if (field?.switchRow?.isOn != value.switchRow.isOn) mSwitch.isChecked = value.switchRow.isOn
                    mSwitch.setOnClickListener {
                        value.clickListener.invoke(value.switchRow.id, field?.switchRow?.isOn == true, mSwitch.isChecked)
                    }
                }
            }
    }


    companion object {
        private var list: ArrayList<SettingRow> = arrayListOf()

        fun new(): SettingsAdapter.Companion {
            list.clear()
            return this
        }

        fun setTitle(id: Any = UUID.randomUUID().toString(),
                     titleId: Int): SettingsAdapter.Companion {
            list.add(TitleRow(id, titleId))
            return this
        }

        fun setDelimeter(): SettingsAdapter.Companion {
            list.add(DelimeterRow())
            return this
        }

        fun setSpace(@DimenRes space: Int): SettingsAdapter.Companion {
            list.add(SpaceRow(space))
            return this
        }

        fun setSwitch(id: Any, textId: Int, isOn: Boolean): SettingsAdapter.Companion {
            list.add(SwitchRow(id, textId, isOn))
            return this
        }

        fun setCheck(id: Any,
                     textId: Int,
                     isChecked: Boolean,
                     @DrawableRes checkedResId: Int): SettingsAdapter.Companion {
            list.add(CheckRow(id, textId, isChecked, checkedResId))
            return this
        }

        fun build(onSwitch: (id: Any, oldValue: Boolean, newValue: Boolean) -> Unit = { _, _, _ -> }): SettingsAdapter {
            val out = SettingsAdapter(list, onSwitch)
            list = arrayListOf()
            return out
        }

        fun build(): List<SettingRow> {
            val out = list.toArrayList()
            list = arrayListOf()
            return out
        }
    }
}

sealed class SettingRow(open val id: Any)

data class SpaceRow(@DimenRes val space: Int, override val id: Any = UUID.randomUUID().toString()) : SettingRow(id)

data class TitleRow(override val id: Any, val titleId: Int) : SettingRow(id)

data class DelimeterRow(val unused: Int = 0, override val id: Any = UUID.randomUUID().toString()) : SettingRow(id)

data class SwitchRow(override val id: Any, val textId: Int, var isOn: Boolean) : SettingRow(id) {
    fun switch(isActivated: Boolean): SwitchRow = SwitchRow(id, textId, isActivated)
}

data class CheckRow(override val id: Any,
                    val textId: Int,
                    val isChecked: Boolean,
                    @DrawableRes val imageReId: Int) : SettingRow(id) {
    fun switch(): CheckRow = this.copy(isChecked = !isChecked)
}