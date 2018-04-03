package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.makeramen.roundedimageview.RoundedImageView
import io.golos.golos.R
import io.golos.golos.utils.*

/**
 * Created by yuri on 08.02.18.
 */
class PhotosDialog : DialogFragment() {
    private lateinit var mPager: ViewPager
    private lateinit var mIndicatorHosts: ViewGroup
    private val mImages: ArrayList<String> = arrayListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_photos, container, false)
        mPager = v.findViewById(R.id.pager)
        mImages.addAll(arguments?.getStringArrayList("images")
                ?: arrayListOf())
        mPager.adapter = PhotosAdapter(childFragmentManager, mImages)
        val position = arguments?.getInt("position") ?: 0
        mPager.post { mPager.setCurrentItem(position, false) }
        v.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { dismiss() }
        v.findViewById<View>(R.id.share).setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val url = mImages.getOrNull(mPager.currentItem)
                activity?.startActivity(url?.asIntentToShowUrl() ?: return)
            }
        })
        mIndicatorHosts = v.findViewById(R.id.indicator_host)
        (0 until mImages.size).forEach { mIndicatorHosts.addView(createIndicator()) }
        if (mImages.size > 0) (mIndicatorHosts[0] as? RoundedImageView)?.setImageResource(android.R.color.white)
        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                val child = mIndicatorHosts[position] as? RoundedImageView ?: return
                child.setImageResource(android.R.color.white)
                mIndicatorHosts.iterator()
                        .forEach {
                            if (it != child && it is RoundedImageView) it.setImageResource(R.color.gray_7d)
                        }
            }
        })
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


    private fun createIndicator(): View? {
        val roundImageView = RoundedImageView(this.context)
        roundImageView.layoutParams = LinearLayoutCompat.LayoutParams(getDimension(R.dimen.six_dp),
                getDimension(R.dimen.six_dp))
        roundImageView.isOval = true
        (roundImageView.layoutParams as LinearLayoutCompat.LayoutParams).marginEnd = getDimension(R.dimen.six_dp)
        roundImageView.setImageResource(R.color.gray_7d)
        return roundImageView
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