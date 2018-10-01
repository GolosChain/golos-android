package io.golos.golos.screens.tags.fragments

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.tags.adapters.SearchTagAdapter
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.tags.views.TagsAndUsersPager
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.MyLinearLayoutManager
import kotlin.reflect.KProperty

class TagsListFragment : GolosFragment() {
    private var mRecycler: androidx.recyclerview.widget.RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_tags_list, container, false)
        mRecycler = v.findViewById(R.id.tags_search_recycler)
        mRecycler?.layoutManager = MyLinearLayoutManager(activity!!)
        mRecycler?.adapter = SearchTagAdapter(onTagClickListener)
        if (tags.isNotEmpty()) {
            (mRecycler?.adapter as? SearchTagAdapter)?.tags = tags
        }
        return v
    }


    operator fun getValue(tagsAndUsesrPager: TagsAndUsersPager, property: KProperty<*>): List<LocalizedTag> = tags

    operator fun setValue(tagsAndUsesrPager: TagsAndUsersPager, property: KProperty<*>, list: List<LocalizedTag>) {
        tags = list
    }

    private var tags: List<LocalizedTag> = arrayListOf()
        set(value) {
            field = value
            (mRecycler?.adapter as? SearchTagAdapter)?.tags = field
        }

    var onTagClickListener: TagsAndUsersPager.OnTagClickListener? = null
}