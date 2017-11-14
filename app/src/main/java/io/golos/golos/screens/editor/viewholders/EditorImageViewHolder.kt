package io.golos.golos.screens.editor.viewholders

import android.support.annotation.LayoutRes
import android.view.ViewGroup
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.editor.CancellableImage
import io.golos.golos.screens.editor.CancellableImageState
import io.golos.golos.screens.editor.EditorAdapterImagePart

class EditorImageViewHolder(@LayoutRes res: Int, parent: ViewGroup) : GolosViewHolder(res, parent) {
    private var mImage: CancellableImage = itemView as CancellableImage
    var state = EditorAdapterImagePart()
        set(value) {
            field = value
            if (field.src.isNotEmpty()) {
                mImage.state = CancellableImageState(field.isLoading, field.src)
                mImage.onImageRemove = {
                    field.onImageDelete(field.id, it ?: "")
                }
            } else {
                mImage.state = null
            }
        }

    init {

    }
}