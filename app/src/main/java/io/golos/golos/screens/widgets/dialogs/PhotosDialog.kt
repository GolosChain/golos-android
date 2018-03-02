package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.utils.bundleOf
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 08.02.18.
 */
class PhotosDialog : DialogFragment() {
    private lateinit var mPager: ViewPager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_photos, container, false)
        mPager = v.findViewById(R.id.pager)
        mPager.adapter = PhotosAdapter(childFragmentManager, arguments?.getStringArrayList("images")
                ?: arrayListOf())
        arguments?.getInt("position")?.let {
            mPager.post { mPager.setCurrentItem(it, false) }
        }
        v.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { dismiss() }
        return v
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(android.support.v4.app.DialogFragment.STYLE_NO_FRAME, R.style.GolosPhotoDialogTheme)
        super.onCreate(savedInstanceState)
    }


    companion object {
        fun getInstance(images: List<String>, position: Int): PhotosDialog {
            val bundle = bundleOf(Pair("images", images.toArrayList()))
            if (position < images.size) {
                bundle.putInt("position", position)
            }
            val f = PhotosDialog()
            f.arguments = bundle
            return f
        }
    }
}

class PhotosAdapter(fm: FragmentManager,
                    private val images: List<String>) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return PhotoFragment.getInstance(images[position])
    }

    override fun getCount(): Int {
        return images.size
    }
}