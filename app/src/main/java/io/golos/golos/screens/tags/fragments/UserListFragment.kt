package io.golos.golos.screens.tags.fragments

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUserWithAvatar
import io.golos.golos.screens.tags.views.TagsAndUsersPager
import io.golos.golos.screens.userslist.UserListAdapter
import io.golos.golos.screens.userslist.model.UserListRowData
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.MyLinearLayoutManager
import kotlin.reflect.KProperty

class UserListFragment : GolosFragment() {
    private var mRecycler: RecyclerView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_user_list, container, false)
        mRecycler = v.findViewById(R.id.user_list_recycler)
        mRecycler?.layoutManager = MyLinearLayoutManager(activity)
        mRecycler?.adapter = UserListAdapter()
        if (userClickListener != null) {
            (mRecycler?.adapter as? UserListAdapter)?.setUserClickListener {
                userClickListener?.onClick(GolosUserWithAvatar(it.name, it.avatar))
            }
        }
        if (mUsers.isNotEmpty()) {
            (mRecycler?.adapter as? UserListAdapter)?.listItems = mUsers.map {
                UserListRowData(it.userName, it.avatarPath, null, null)
            }
        }
        return v
    }

    operator fun getValue(tagsAndUsersPager: TagsAndUsersPager, property: KProperty<*>) = mUsers

    operator fun setValue(tagsAndUsersPager: TagsAndUsersPager, property: KProperty<*>, list: List<GolosUserWithAvatar>) {
        mUsers = list
        (mRecycler?.adapter as? UserListAdapter)?.listItems = mUsers.map {
            UserListRowData(it.userName, it.avatarPath, null, null)
        }
    }

    var userClickListener: TagsAndUsersPager.OnUserClickListener? = null
        set(value) {
            field = value
            (mRecycler?.adapter as? UserListAdapter)?.setUserClickListener {
                value?.onClick(GolosUserWithAvatar(it.name, it.avatar))
            }
        }


    private var mUsers: List<GolosUserWithAvatar> = arrayListOf()
}