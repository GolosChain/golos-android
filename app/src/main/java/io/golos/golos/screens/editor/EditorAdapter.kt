package io.golos.golos.screens.editor

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.utils.StringValidator
import io.golos.golos.screens.editor.viewholders.EditorAdapterFooterViewHolder
import io.golos.golos.screens.editor.viewholders.EditorEditTextViewHolder
import io.golos.golos.screens.editor.viewholders.EditorTitleViewHolder

/**
 * Created by yuri yurivladdurain@gmail.com on 27/10/2017.
 *
 */
abstract class EditorAdapterModel(open val id: String)

data class EditorAdapterTextPart(override val id: String = "",
                                 var text: CharSequence = "",
                                 var currentCursorPosition: Int? = null,
                                 val onFocusChanged: (Boolean) -> Unit = {},
                                 val onNewText: (String, CharSequence) -> Unit = { _, _ -> }) : EditorAdapterModel(id)

data class EditorAdapterImagePart(override val id: String = "",
                                  val src: String = "",
                                  val isLoading: Boolean = false,
                                  val onImageDelete: (String, String)
                                  /** first id, second - src **/
                                  -> Unit = { _, _ -> }) : EditorAdapterModel(id)

data class EditorAdapterHeader(var title: CharSequence = "",
                               val isTitleEditable: Boolean = false,
                               val onTitleChanges: (CharSequence) -> Unit = {},
                               val subtitle: CharSequence? = null) : EditorAdapterModel("header")

data class EditorAdapterFooter(val showTagsEditor: Boolean = false,
                               val tagsValidator: StringValidator? = null,
                               var tags: ArrayList<String> = ArrayList()) : EditorAdapterModel("footer")

interface EditorAdapterInteractions {
    fun onEdit(parts: List<Part>, title: String, tags: List<String>)
    fun onSubmit(parts: List<Part>, title: String, tags: List<String>)
    fun onLinkRequest(parts: List<Part>, title: String, tags: List<String>)
    fun onPhotoRequest(parts: List<Part>, title: String, tags: List<String>)
    fun onPhotoDelete(image: EditorImagePart, parts: List<Part>, title: String, tags: List<String>)
}

class EditorAdapter(var items: ArrayList<Part> = ArrayList(),
                    titleText: CharSequence = "",
                    isTitleEditable: Boolean = false,
                    subititleText: CharSequence? = null,
                    showTagsEditor: Boolean = false,
                    var tagsValidator: StringValidator? = null,
                    var interactor: EditorAdapterInteractions? = null)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var models: List<EditorAdapterModel> = ArrayList()
    var currentTitle = ""
    var currentTags = ArrayList<String>()

    init {
        val out = ArrayList<EditorAdapterModel>()
        items.forEachIndexed { index, part ->
            if (part is EditorTextPart) {
                out.add(EditorAdapterTextPart(part.id, part.text, part.pointerPosition, {
                    val item = out.find { it.id == part.id } as EditorAdapterTextPart
                    items[index] = EditorTextPart(item.text.toString(), null)
                }))
            } else if (part is EditorImagePart) {
                out.add(EditorAdapterImagePart(part.id, part.imageUrl, false, { _, _ ->
                    interactor?.onPhotoDelete(part, items, currentTitle, currentTags)
                }))
            }
        }
        out.add(0, EditorAdapterHeader(titleText, isTitleEditable, { currentTitle = it.toString() }, subititleText))
        out.add(EditorAdapterFooter(showTagsEditor, tagsValidator))
        models = ArrayList(out)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder == null) return
        when (holder) {
            is EditorTitleViewHolder -> holder.state = models[0] as EditorAdapterHeader
            is EditorAdapterFooterViewHolder -> holder.state = models.last() as EditorAdapterFooter
            is EditorEditTextViewHolder -> holder.state = models[position] as EditorAdapterTextPart
        }
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> EditorTitleViewHolder(R.layout.vh_editor_title, parent!!)
            1 -> EditorAdapterFooterViewHolder(R.layout.vh_editor_footer, parent!!)
            2 -> EditorEditTextViewHolder(R.layout.vh_edit_text, parent!!)
            else -> throw IllegalStateException("unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return 0
        if (position == models.lastIndex) return 1
        if (models[position] is EditorAdapterTextPart) return 2
        return if (models[position] is EditorAdapterImagePart) 3
        else 4
    }
}

