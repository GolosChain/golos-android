package io.golos.golos.screens.tags

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.wefika.flowlayout.FlowLayout
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.tags.viewmodel.AddTagTofilterViewModel
import io.golos.golos.screens.tags.viewmodel.FiltersScreenState
import io.golos.golos.utils.setTextColorHint
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible

/**
 * Created by yuri on 08.01.18.
 */


class TagsCloudActivity : GolosActivity(), Observer<FiltersScreenState> {
    private lateinit var mViewModel: AddTagTofilterViewModel
    private lateinit var mUserSubscibedTagsOn: FlowLayout
    private lateinit var mALltagsLo: FlowLayout
    private lateinit var mAppBar: View
    private lateinit var mScrolllLO: View
    private lateinit var mProgress: View
    private lateinit var mSubscribedLbl: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tags_cloud)
        mViewModel = ViewModelProviders.of(this).get(AddTagTofilterViewModel::class.java)
        setup()
        mViewModel.onCreate()
        mViewModel.getLiveData().observe(this, this)
    }

    private fun setup() {
        findViewById<View>(R.id.back_btn).setOnClickListener { onBackPressed() }
        val sv = findViewById<android.support.v7.widget.SearchView>(R.id.search_view)
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                mViewModel.search(p0 ?: "")
                return true
            }
        })
        sv.setOnCloseListener {
            mViewModel.onSearchEnd()
            false
        }
        sv.setOnQueryTextFocusChangeListener({ v, isFocused ->
            if (v == sv && isFocused && sv.query.isEmpty()) mViewModel.onSearchStart()
            else if (v == sv && !isFocused && sv.query.isEmpty()) mViewModel.onSearchEnd()
        })
        sv.setTextColorHint(R.color.textColorP)
        mUserSubscibedTagsOn = findViewById(R.id.subscribed_tags)
        mALltagsLo = findViewById(R.id.all_tags_lo)
        mAppBar = findViewById(R.id.appbar)
        mProgress = findViewById(R.id.progress)
        mScrolllLO = findViewById(R.id.scroll_lo)
        mSubscribedLbl = findViewById(R.id.subscribed_lbl)
        sv.setFocusable(false)
    }

    override fun onChanged(t: FiltersScreenState?) {
        if (t?.isLoading == true) {
            mAppBar.visibility = View.GONE
            mScrolllLO.visibility = View.GONE
            mProgress.visibility = View.VISIBLE
        } else {
            mAppBar.visibility = View.VISIBLE
            mScrolllLO.visibility = View.VISIBLE
            mProgress.visibility = View.GONE
        }
        if (t?.subscribedTags?.size ?: 0 == 0) {
            mSubscribedLbl.setViewGone()
            mUserSubscibedTagsOn.setViewGone()
        } else {
            mSubscribedLbl.setViewVisible()
            mUserSubscibedTagsOn.setViewVisible()
        }
        t?.subscribedTags?.let {
            val lastIndexOfButton = mUserSubscibedTagsOn.childCount - 1
            it.forEachIndexed { index, tag ->
                if (index > lastIndexOfButton) {
                    mUserSubscibedTagsOn.addView(createSubscribedTag(tag), index)
                } else {
                    val v = mUserSubscibedTagsOn.getChildAt(index)
                    v.findViewById<TextView>(R.id.tag_name).text = tag.getLocalizedName()
                    v.tag = tag
                }
            }
            if (mUserSubscibedTagsOn.childCount > it.size) {
                if (it.isEmpty()) mUserSubscibedTagsOn.removeAllViews()
                else {
                    mUserSubscibedTagsOn.removeViews(it.size, mUserSubscibedTagsOn.childCount - it.size)
                }

            }
        }
        t?.shownTags?.let {
            val lastIndexOfButton = mALltagsLo.childCount - 1
            it.forEachIndexed { index, tag ->
                if (index < 31) {
                    if (index > lastIndexOfButton) {
                        mALltagsLo.addView(createCloudTag(tag))
                    } else {
                        val v = mALltagsLo.getChildAt(index)
                        v.findViewById<TextView>(R.id.tag_name).text = tag.getLocalizedName()
                        v.tag = tag
                    }
                }
            }
            if (mALltagsLo.childCount > it.size) {
                mALltagsLo.removeViews(it.size, mALltagsLo.childCount - it.size)
            }
        }

    }

    private fun createSubscribedTag(tag: LocalizedTag): View {
        val view = layoutInflater.inflate(R.layout.vh_tag_white, mUserSubscibedTagsOn, false)
        view.tag = tag
        val textView = view.findViewById<TextView>(R.id.tag_name)
        textView.text = tag.getLocalizedName()
        textView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                mViewModel.onTagClick(this@TagsCloudActivity, view?.tag as? LocalizedTag ?: return)
            }
        })
        val minus = view.findViewById<View>(R.id.minus_ibtn)
        minus.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                mViewModel.onTagUnSubscribe(view?.tag as? LocalizedTag ?: return)
            }
        })
        val params = FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                resources.getDimension(R.dimen.tags_reycler_height).toInt())
        params.rightMargin = resources.getDimension(R.dimen.margin_material_half).toInt()
        params.bottomMargin = resources.getDimension(R.dimen.margin_material_small).toInt()
        view.layoutParams = params
        return view
    }

    private fun createCloudTag(tag: LocalizedTag): View {
        val view = layoutInflater.inflate(R.layout.vh_tag_blue, mALltagsLo, false)
        view.tag = tag
        val textView = view.findViewById<TextView>(R.id.tag_name)
        textView.text = tag.getLocalizedName()
        textView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                mViewModel.onTagClick(this@TagsCloudActivity, view?.tag as? LocalizedTag ?: return)
            }
        })
        val minus = view.findViewById<View>(R.id.plus_btn)
        minus.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                mViewModel.onTagSubscribe(view?.tag as? LocalizedTag ?: return)
            }
        })
        val params = FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                resources.getDimension(R.dimen.tags_reycler_height).toInt())
        params.rightMargin = resources.getDimension(R.dimen.margin_material_half).toInt()
        params.bottomMargin = resources.getDimension(R.dimen.margin_material_small).toInt()
        view.layoutParams = params

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val i = Intent(context, TagsCloudActivity::class.java)
            context.startActivity(i)
        }
    }
}