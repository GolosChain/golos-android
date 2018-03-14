package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.ImageUriResolver
import io.golos.golos.utils.bundleOf
import io.golos.golos.utils.nextInt

/**
 * Created by yuri on 02.03.18.
 */
class PhotoFragment : GolosFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = PhotoView(activity)
        v.id = nextInt()
        val src = arguments?.getString("image") ?: ""
        Glide.with(v)
                .load(ImageUriResolver.resolveImageWithSize(src, wantedwidth = 0))
                .apply(RequestOptions()
                        .placeholder(R.drawable.error)
                        .error(R.drawable.error)
                        .override(activity?.resources?.displayMetrics?.widthPixels ?: 768
                        * 2, Target.SIZE_ORIGINAL))
                .into(v)
        return v
    }

    companion object {
        fun getInstance(uri: String): PhotoFragment {
            val b = bundleOf(Pair("image", uri))
            val f = PhotoFragment()
            f.arguments = b
            return f
        }
    }
}