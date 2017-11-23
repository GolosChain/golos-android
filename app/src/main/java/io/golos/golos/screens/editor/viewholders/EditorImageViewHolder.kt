package io.golos.golos.screens.editor.viewholders

import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.editor.CancellableImage
import io.golos.golos.screens.editor.CancellableImageState
import io.golos.golos.screens.editor.EditorAdapterImagePart
import io.golos.golos.screens.widgets.GolosViewHolder

class EditorImageViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_editor_image, parent) {
    private val mImage: CancellableImage = itemView.findViewById(R.id.cancelable_image)
    var state: EditorAdapterImagePart? = null
        set(value) {
            field = value
            val src = field?.imagePart?.imageUrl
            if (src != null && src.isNotEmpty()) {
                mImage.state = CancellableImageState(false, src)
                mImage.onImageRemove = {
                    field?.onImageDelete?.invoke(this, field?.id ?: "", src)
                }
            } else {
                mImage.state = null
            }
        }
}