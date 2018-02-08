package io.golos.golos.screens.widgets

import android.app.DialogFragment
import android.os.Bundle
import android.view.ViewGroup
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 08.02.18.
 */
class PhotosDialog : DialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }


    companion object {
        fun getInstance(images: List<String>): PhotosDialog {
            val bundle = Bundle()
            bundle.putStringArrayList("images", images.toArrayList())
            val f = PhotosDialog()
            f.arguments = bundle
            return f
        }
    }
}