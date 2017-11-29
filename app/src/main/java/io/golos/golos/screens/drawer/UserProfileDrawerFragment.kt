package io.golos.golos.screens.drawer

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.androidviewmodel.AuthViewModel
import io.golos.golos.screens.drawer.adapters.MenuAdapter
import io.golos.golos.screens.drawer.adapters.MenuItem

/**
 * Created by yuri on 10.11.17.
 */
class UserProfileDrawerFragment : Fragment() {
    private lateinit var mUserAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mPostsAndMonetTv: TextView
    private lateinit var mMenuList: ListView
    private lateinit var mViewModel: AuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fr_user_logged_in, container, false)
        mUserAvatar = v.findViewById(R.id.avatar_iv)
        mUserName = v.findViewById(R.id.username_tv)
        mPostsAndMonetTv = v.findViewById(R.id.posts_and_money)
        mMenuList = v.findViewById(R.id.menu_list)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        val adapter = MenuAdapter(listOf(MenuItem(getString(R.string.logout), R.drawable.ic_logout_40dp_gray, 0)), { _ -> mViewModel.onLogoutClick() }, activity!!)
        mMenuList.adapter = adapter
        mViewModel.userProfileState.observe(this, android.arch.lifecycle.Observer {
            mUserName.text = it?.userName
            val glide = Glide.with(view)
            if (it?.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
            else {
                glide.load(it.avatarPath)
                        .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                        .error(glide.load(R.drawable.ic_person_gray_80dp))
                        .into(mUserAvatar)
            }
            mUserName.text = "@${it?.userName}"
            mPostsAndMonetTv.text = "${it?.userPostsCount} - \$ ${String.format("%.2f", it?.userMoney ?: 0.0)}"
        })
    }

    companion object {
        fun getInstance(): UserProfileDrawerFragment {
            val f = UserProfileDrawerFragment()
            return f
        }
    }
}


