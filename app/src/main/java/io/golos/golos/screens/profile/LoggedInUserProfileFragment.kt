package io.golos.golos.screens.profile

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.profile.adapters.ProfileFragmentsAdapter
import io.golos.golos.screens.settings.SettingActivity

/**
 * Created by yuri on 10.11.17.
 */
class LoggedInUserProfileFragment : Fragment(), Observer<UserProfileState> {
    private lateinit var mUserAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mViewModel: AuthViewModel
    private lateinit var mSettingButton: ImageButton
    private lateinit var mMotoTv: TextView
    private lateinit var mSubscribersNumTv: TextView
    private lateinit var mSubscriptionsNum: TextView
    private lateinit var mSubscribers: TextView
    private lateinit var mSubscriptions: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_user_logged_in, container, false)
        mUserAvatar = v.findViewById(R.id.avatar_iv)
        mUserName = v.findViewById(R.id.username_tv)
        mSettingButton = v.findViewById(R.id.settings_btn)
        mMotoTv = v.findViewById(R.id.moto)
        mSubscribersNumTv = v.findViewById(R.id.subscribers_num_tv)
        mSubscriptionsNum = v.findViewById(R.id.subscriptions_num_tv)
        mSubscribers = v.findViewById(R.id.subscribers_tv)
        mSubscriptions = v.findViewById(R.id.subscribes_tv)
        val pager = v.findViewById<ViewPager>(R.id.profile_pager)
        val adapter = ProfileFragmentsAdapter(childFragmentManager, activity!!)
        pager.offscreenPageLimit = adapter.size
        pager.adapter = adapter
        val tabbar = v.findViewById<TabLayout>(R.id.tab_lo_logged_in)
        tabbar.setupWithViewPager(pager)
        v.findViewById<View>(R.id.settings_btn).setOnClickListener({
            val i = Intent(activity!!, SettingActivity::class.java)
            activity?.startActivity(i)
        })
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        mViewModel.userProfileState.observe(activity as LifecycleOwner, this)
    }

    override fun onChanged(it: UserProfileState?) {
        if (view == null) return
        mUserName.text = it?.userName?.capitalize()
        val glide = Glide.with(view ?: return)
        if (it?.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
        else {
            glide.load(it.avatarPath)
                    .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                    .error(glide.load(R.drawable.ic_person_gray_80dp))
                    .into(mUserAvatar)
        }
        mMotoTv.text = it?.userMoto
        mSubscribersNumTv.text = it?.subscribersNum?.toString()
        mSubscriptionsNum.text = it?.subscribesNum?.toString()
        mSubscribers.text = resources.getQuantityString(R.plurals.subscribers, it?.subscribersNum?.toInt() ?: 0)
        mSubscriptions.text = resources.getQuantityString(R.plurals.subscription, it?.subscribesNum?.toInt() ?: 0)
    }

    companion object {
        fun getInstance(): LoggedInUserProfileFragment {
            val f = LoggedInUserProfileFragment()
            return f
        }
    }
}


