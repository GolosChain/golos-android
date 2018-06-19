package io.golos.golos.screens.editor

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.editor.viewholders.EditorEditTextViewHolder
import io.golos.golos.screens.editor.viewholders.EditorImageViewHolder
import timber.log.Timber
import java.util.*


/**
 * Created by yuri yurivladdurain@gmail.com on 27/10/2017.
 *
 */
abstract class EditorAdapterModel(open val id: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditorAdapterModel) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "EditorAdapterModel(id='$id')"
    }

}

data class EditorAdapterTextPart(val textPart: EditorTextPart,
                                 val onFocusChanged: (RecyclerView.ViewHolder, Boolean) -> Unit = { _, _ -> },
                                 val onNewText: (RecyclerView.ViewHolder, EditorTextPart) -> Unit = { _, _ -> },
                                 val onCursorChange: (RecyclerView.ViewHolder, EditorTextPart) -> Unit = { _, _ -> },
                                 val showHint: Boolean = false,
                                 val modifiers: Set<EditorTextModifier>) : EditorAdapterModel(textPart.id)

data class EditorAdapterImagePart(val imagePart: EditorImagePart,
                                  val isLoading: Boolean = false,
                                  val onImageDelete: (RecyclerView.ViewHolder, String, String)
                                  /** second - part id, third - src **/
                                  -> Unit = { _, _, _ -> }) : EditorAdapterModel(imagePart.id)

interface EditorAdapterInteractions {
    fun onEdit(parts: List<EditorPart>)
    fun onPhotoDelete(image: EditorImagePart, parts: List<EditorPart>)
    fun onCursorChange(parts: List<EditorPart>)
}

class EditorAdapter(var interactor: EditorAdapterInteractions? = null)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var parts: ArrayList<EditorPart> = ArrayList()
        set(value) {
            Timber.e("setvalue")
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition].id == value[newItemPosition].id
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = value.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = field[oldItemPosition]
                    val new = value[newItemPosition]
                    return old == new
                }
            }).dispatchUpdatesTo(this)
            field = value

        }
    var textModifiers: Set<EditorTextModifier> = hashSetOf()
        set(value) {
            field = value
            (0 until parts.size).forEach {
                if (parts[it] is EditorTextPart) notifyItemChanged(it)
            }
        }

    @Suppress("NAME_SHADOWING")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EditorEditTextViewHolder -> {
                val part = parts[position] as EditorTextPart
                holder.state = EditorAdapterTextPart(part, { holder, isFocused ->
                    if (!isFocused) {
                        val position = holder.adapterPosition
                        if (position > 0)
                            (parts.getOrNull(position) as? EditorTextPart)?.setNotSelected()
                    }
                }, { _, changingPart ->
                    (0 until parts.size)
                            .filter {
                                parts[it].id == changingPart.id
                            }
                            .forEach {
                                parts[it] = changingPart
                            }
                    interactor?.onEdit(parts)
                }, { _, _ ->

                    interactor?.onCursorChange(parts)
                },
                        position == 0,
                        textModifiers)
                if (parts[position].isFocused()) {
                    holder.shouldShowKeyboard()
                }
            }
            is EditorImageViewHolder -> {
                val part = parts[position] as EditorImagePart
                holder.state = EditorAdapterImagePart(part, false, { a, _, _ ->
                    interactor?.onPhotoDelete(parts[a.adapterPosition] as EditorImagePart, parts)
                })
            }
        }
    }

    override fun getItemCount(): Int {
        return parts.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> EditorEditTextViewHolder(R.layout.vh_edit_text, parent)
            1 -> EditorImageViewHolder(parent)
            else -> throw IllegalStateException("unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (parts[position] is EditorTextPart) 0
        else 1
    }

    fun onRequestFocus(mRecycler: RecyclerView) {

        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part?.isFocused() == true)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.shouldShowKeyboard()
        }
    }
}

