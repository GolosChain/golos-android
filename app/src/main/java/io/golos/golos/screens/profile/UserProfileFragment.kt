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
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.screens.profile.adapters.ProfileFragmentsAdapter
import io.golos.golos.screens.profile.viewmodel.UserAccountModel
import io.golos.golos.screens.profile.viewmodel.UserInfoViewModel
import io.golos.golos.screens.settings.SettingActivity
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.showSnackbar

/**
 * Created by yuri on 10.11.17.
 */
class UserProfileFragment : Fragment(), Observer<UserAccountModel> {
    private lateinit var mUserAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mViewModel: UserInfoViewModel
    private lateinit var mSettingButton: ImageButton
    private lateinit var mMotoTv: TextView
    private lateinit var mSubscribersNumTv: TextView
    private lateinit var mSubscriptionsNum: TextView
    private lateinit var mSubscribers: TextView
    private lateinit var mSubscriptions: TextView
    private lateinit var mPostsTv: TextView
    private lateinit var mPostsCountTv: TextView
    private lateinit var mFollowBtn: Button
    private lateinit var mFollowProgress: View
    private lateinit var mSubscribersBtn: View
    private lateinit var mSubscriptionsBtn: View


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
        mPostsTv = v.findViewById(R.id.posts_tv)
        mPostsCountTv = v.findViewById(R.id.posts_num_tv)
        mFollowBtn = v.findViewById(R.id.follow_btn)
        mFollowProgress = v.findViewById(R.id.progress)
        mSubscriptionsBtn = v.findViewById(R.id.subscriptions_lo)
        mSubscribersBtn = v.findViewById(R.id.subscribers_lo)
        val pager = v.findViewById<ViewPager>(R.id.profile_pager)
        if (arguments?.containsKey(USERNAME_TAG) == true) {
            val adapter = ProfileFragmentsAdapter(childFragmentManager,
                    activity!!,
                    arguments!!.getString(USERNAME_TAG))
            pager.offscreenPageLimit = adapter.size
            pager.adapter = adapter

        }

        val tabbar = v.findViewById<TabLayout>(R.id.tab_lo_logged_in)
        tabbar.setupWithViewPager(pager)
        val settingsButton = v.findViewById<View>(R.id.settings_btn)
        settingsButton.setOnClickListener({
            val i = Intent(activity!!, SettingActivity::class.java)
            activity?.startActivity(i)
        })
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity!!).get(UserInfoViewModel::class.java)
        mViewModel.getLiveData().observe(activity as LifecycleOwner, this)
        if (arguments?.containsKey(USERNAME_TAG) == true) {
            mViewModel.onCreate(arguments!!.getString(USERNAME_TAG),
                    object : InternetStatusNotifier {
                        override fun isAppOnline(): Boolean {
                            return App.isAppOnline()
                        }
                    })
            mSubscribersBtn.setOnClickListener {
                mViewModel.onSubscriberClick(activity ?: return@setOnClickListener, (arguments!!.getString(USERNAME_TAG)))
            }
            mSubscriptionsBtn.setOnClickListener {
                mViewModel.onSubscriptionsClick(activity ?: return@setOnClickListener, (arguments!!.getString(USERNAME_TAG)))
            }
        }
        mFollowBtn.setOnClickListener({ mViewModel.onFollowBtnClick() })
    }

    override fun onChanged(t: UserAccountModel?) {
        if (view == null) return
        val it = t?.accountInfo ?: return
        mUserName.text = it.userName?.capitalize()
        val glide = Glide.with(view ?: return)
        if (it.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
        else {
            glide.load(it.avatarPath)
                    .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                    .error(glide.load(R.drawable.ic_person_gray_80dp))
                    .into(mUserAvatar)
        }

        if (it.isCurrentUserSubscribed) mFollowBtn.text = getString(R.string.unfollow)
        else mFollowBtn.text = getString(R.string.follow)
        mSettingButton.visibility = if (t.isActiveUserPage) View.VISIBLE else View.GONE
        mFollowBtn.visibility = if (t.isFollowButtonVisible && !t.isSubscriptionInProgress) View.VISIBLE else View.GONE
        mFollowProgress.visibility = if (t.isSubscriptionInProgress) View.VISIBLE else View.GONE
        mMotoTv.text = it.userMotto
        mSubscribersNumTv.text = it.subscribersCount.toString()
        mSubscriptionsNum.text = it.subscibesCount.toString()
        mPostsCountTv.text = it.postsCount.toString()
        mSubscribers.text = resources.getQuantityString(R.plurals.subscribers, it.subscribersCount.toInt())
        mSubscriptions.text = resources.getQuantityString(R.plurals.subscription, it.subscibesCount.toInt())
        mPostsTv.text = resources.getQuantityString(R.plurals.posts, it.postsCount.toInt())
        t.error?.let {
            view?.showSnackbar(it.localizedMessage ?: 0)
        }
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        fun getInstance(username: String): UserProfileFragment {
            val b = Bundle()
            b.putString(USERNAME_TAG, username)
            val f = UserProfileFragment()
            f.arguments = b
            return f
        }
    }
}


