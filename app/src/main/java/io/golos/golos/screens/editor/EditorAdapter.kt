package io.golos.golos.screens.editor

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.text.Selection
import android.view.ViewGroup
import io.golos.golos.BuildConfig.DEBUG_EDITOR
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
                                 val onFocusChanged: (androidx.recyclerview.widget.RecyclerView.ViewHolder, Boolean) -> Unit = { _, _ -> },
                                 val onNewText: (androidx.recyclerview.widget.RecyclerView.ViewHolder, EditorTextPart) -> Unit = { _, _ -> },
                                 val onCursorChange: (androidx.recyclerview.widget.RecyclerView.ViewHolder, EditorTextPart) -> Unit = { _, _ -> },
                                 val showHint: Boolean = false) : EditorAdapterModel(textPart.id)

data class EditorAdapterImagePart(val imagePart: EditorImagePart,
                                  val isLoading: Boolean = false,
                                  val onImageDelete: (androidx.recyclerview.widget.RecyclerView.ViewHolder, String, String)
                                  /** second - part id, third - src **/
                                  -> Unit = { _, _, _ -> }) : EditorAdapterModel(imagePart.id)

interface EditorAdapterInteractions {
    fun onEdit(parts: List<EditorPart>)
    fun onPhotoDelete(image: EditorImagePart, parts: List<EditorPart>)
    fun onCursorChange(parts: List<EditorPart>)
}

class EditorAdapter(var interactor: EditorAdapterInteractions? = null)
    : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    private val callback = MyCallback()

    var parts: ArrayList<EditorPart> = ArrayList()
        set(value) {
            if (DEBUG_EDITOR) Timber.e("setvalue")
            callback.newList = value
            callback.oldList = field
            DiffUtil.calculateDiff(callback).dispatchUpdatesTo(this)
            field = value
        }

    private class MyCallback : DiffUtil.Callback() {
        var newList = listOf<EditorPart>()
        var oldList = listOf<EditorPart>()


        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]

        }
    }

    fun onTextSizeChanged(mRecycler: androidx.recyclerview.widget.RecyclerView) {
        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part?.isFocused() == true)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.reSetText()
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EditorEditTextViewHolder -> {
                val part = parts[position] as EditorTextPart
                holder.state = EditorAdapterTextPart(part, { holder, isFocused ->
                    if (!isFocused) {
                        val position = holder.adapterPosition
                        if (position > 0)
                            (parts.getOrNull(position) as? EditorTextPart)?.setNotSelected()
                    }
                    interactor?.onCursorChange(parts)
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
                        position == 0)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
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

    fun onRequestFocus(mRecycler: androidx.recyclerview.widget.RecyclerView) {

        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part?.isFocused() == true)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.shouldShowKeyboard()
        }
    }

    fun focusFirstTextPart(mRecycler: androidx.recyclerview.widget.RecyclerView) {

        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part != null) {
                Selection.setSelection(part.text, 0, 0)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.shouldShowKeyboard()
                return@focusFirstTextPart
            }
        }
    }

    fun beforeTextSizeChange(mRecycler: androidx.recyclerview.widget.RecyclerView) {
        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part?.isFocused() == true)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.beforeTextSizeChange()
        }
    }

    fun afterTextSizeChange(mRecycler: androidx.recyclerview.widget.RecyclerView) {
        (0 until itemCount).forEach {
            val part = parts[it] as? EditorTextPart
            if (part?.isFocused() == true)
                (mRecycler.findViewHolderForAdapterPosition(it) as? EditorEditTextViewHolder)?.afterTextSizeChange()
        }
    }
}

