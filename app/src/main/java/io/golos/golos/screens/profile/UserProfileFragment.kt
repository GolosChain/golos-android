package io.golos.golos.screens.profile

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayout
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.GolosActivity.Companion.CHANGE_THEME
import io.golos.golos.screens.profile.adapters.ProfileFragmentsAdapter
import io.golos.golos.screens.profile.viewmodel.UserAccountModel
import io.golos.golos.screens.profile.viewmodel.UserInfoViewModel
import io.golos.golos.screens.settings.SettingsActivity
import io.golos.golos.utils.*

/**
 * Created by yuri on 10.11.17.
 */
class UserProfileFragment : Fragment(), Observer<UserAccountModel>, ReselectionEmitter {
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
    private lateinit var mWalletBalanceLo: View
    private lateinit var mSubscriptionsBtn: View
    private lateinit var mAvatarOverlay: View
    private lateinit var mVotingPowerLo: View
    private lateinit var mVotingPowerTv: TextView
    private lateinit var mVotingPowerIndicator: ProgressBar
    private lateinit var mGolosCountTV: TextView
    private lateinit var mProceedButton: View
    private lateinit var mUserCoverIv: ImageView
    private lateinit var mProfilePager: ViewPager
    private var mLastAccountInfo: GolosUserAccountInfo? = null
    private var mLastAvatarPath: String? = null
    private val mReselectionsEmitter = MutableLiveData<Int>()

    override val reselectLiveData: LiveData<Int>
        get() = mReselectionsEmitter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_user_logged_in, container, false)
        mUserAvatar = v.findViewById(R.id.avatar_iv)
        mUserName = v.findViewById(R.id.username_tv)
        mSettingButton = v.findViewById(R.id.settings_btn)
        mMotoTv = v.findViewById(R.id.moto)
        mSubscribersNumTv = v.findViewById(R.id.subscribers_num_tv)
        mSubscriptionsNum = v.findViewById(R.id.subscriptions_num_tv)
        mUserCoverIv = v.findViewById(R.id.user_backing)
        mSubscribers = v.findViewById(R.id.subscribers_tv)
        mSubscriptions = v.findViewById(R.id.subscribes_tv)
        mProceedButton = v.findViewById(R.id.proceed_btn)
        mPostsTv = v.findViewById(R.id.posts_tv)
        mPostsCountTv = v.findViewById(R.id.posts_num_tv)
        mFollowBtn = v.findViewById(R.id.follow_btn)
        mFollowProgress = v.findViewById(R.id.progress)
        mSubscriptionsBtn = v.findViewById(R.id.subscriptions_lo)
        mWalletBalanceLo = v.findViewById(R.id.wallet_balance_lo)
        mSubscribersBtn = v.findViewById(R.id.subscribers_lo)
        mAvatarOverlay = v.findViewById(R.id.avatar_overlay)
        mVotingPowerLo = v.findViewById(R.id.voting_power_lo)
        mVotingPowerTv = v.findViewById(R.id.voting_power_tv)
        mVotingPowerIndicator = v.findViewById(R.id.voting_power_progress)
        mGolosCountTV = v.findViewById(R.id.tv_wallet_balance_count)
        mProfilePager = v.findViewById<ViewPager>(R.id.profile_pager)
        if (arguments?.containsKey(USERNAME_TAG) == true) {
            val adapter = ProfileFragmentsAdapter(childFragmentManager,
                    activity!!,
                    arguments!!.getString(USERNAME_TAG))
            mProfilePager.offscreenPageLimit = 1
            mProfilePager.adapter = adapter

        }

        val tabbar = v.findViewById<TabLayout>(R.id.tab_lo_logged_in)
        tabbar.setupWithViewPager(mProfilePager)
        val settingsButton = v.findViewById<View>(R.id.settings_btn)
        settingsButton.setOnClickListener {
            val i = Intent(activity!!, SettingsActivity::class.java)
            activity?.startActivityForResult(i, CHANGE_THEME)
        }
        tabbar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                mReselectionsEmitter.value = tabbar.selectedTabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

            }
        })
        return v
    }

    override fun onStart() {
        super.onStart()
        mViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        mViewModel.onStop()
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
                mViewModel.onSubscriberClick(activity
                        ?: return@setOnClickListener, (arguments!!.getString(USERNAME_TAG)))
            }
            mSubscriptionsBtn.setOnClickListener {
                mViewModel.onSubscriptionsClick(activity
                        ?: return@setOnClickListener, (arguments!!.getString(USERNAME_TAG)))
            }
        }
        mFollowBtn.setOnClickListener({ mViewModel.onFollowBtnClick() })
        mSettingButton.visibility = if (mViewModel.isSettingButtonShown()) View.VISIBLE else View.GONE
        mFollowBtn.visibility = if (mViewModel.isFollowButtonVisible()) View.VISIBLE else View.GONE


        mUserAvatar.setOnClickListener {
            mVotingPowerIndicator.progress = 0
            mAvatarOverlay.setViewVisible()
            mVotingPowerIndicator.setViewVisible()
            mVotingPowerTv.text = ""
            mVotingPowerLo.setViewVisible()
            val votingPower: Double = (mViewModel.getLiveData().value?.accountInfo?.votingPower
                    ?: 0) / 100.0

            val delay = 300L
            val duration = 600L

            ObjectAnimator
                    .ofInt(mVotingPowerIndicator, "progress", 0, votingPower.toInt())
                    .setDuration(duration)
                    .setStartDelayB(delay)
                    .setInterpolatorB(AccelerateDecelerateInterpolator())
                    .start()
            ObjectAnimator
                    .ofObject(TypeEvaluator<Float> { p0, _, p2 ->
                        mVotingPowerTv.text = "${String.format("%.2f", p0 * p2)}%"
                        p0
                    },
                            0.0f, votingPower.toFloat()).setDuration(duration)
                    .setStartDelayB(delay)
                    .setInterpolatorB(AccelerateDecelerateInterpolator())
                    .start()

        }
        mVotingPowerIndicator.setOnClickListener {
            mAvatarOverlay.setViewGone()
            mVotingPowerIndicator.setViewGone()
            mVotingPowerLo.setViewGone()
        }

        mWalletBalanceLo.setOnClickListener {
            WalletActivity.start(activity
                    ?: return@setOnClickListener, arguments?.getString(USERNAME_TAG)
                    ?: return@setOnClickListener)
        }

        (activity as? ProfileSectionPreselector)?.getPreselect()?.observe(this, Observer { feedType ->
            mProfilePager.post {
                (mProfilePager.adapter as? ProfileFragmentsAdapter)?.let {
                    mProfilePager.setCurrentItem(it.getPositionOf(feedType) ?: return@let, true)
                }
            }
        })

    }

    override fun onChanged(t: UserAccountModel?) {
        if (view == null) return
        val it = t?.accountInfo ?: return

        mUserName.text = it.userName.capitalize()
        val glide = Glide.with(view ?: return)

        if (it.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
        else if (mLastAvatarPath != t.accountInfo.avatarPath) {

            glide.load(ImageUriResolver.resolveImageWithSize(it.avatarPath, wantedwidth = mUserAvatar.width))
                    .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                    .error(glide.load(R.drawable.ic_person_gray_80dp))
                    .into(mUserAvatar)

            mLastAvatarPath = t.accountInfo.avatarPath
        }

        mFollowBtn.text = getString(t.followButtonText)

        mFollowBtn.visibility = if (t.isFollowButtonVisible && !t.isSubscriptionInProgress) View.VISIBLE else View.GONE
        mFollowProgress.visibility = if (t.isSubscriptionInProgress) View.VISIBLE else View.GONE
        mMotoTv.text = it.userMotto
        mSubscribersNumTv.text = it.subscribersCount.toString()
        mSubscriptionsNum.text = it.subscriptionsCount.toString()
        mPostsCountTv.text = it.postsCount.toString()
        mSubscribers.text = resources.getQuantityString(R.plurals.subscribers, it.subscribersCount.toInt())
        mSubscriptions.text = resources.getQuantityString(R.plurals.subscription, it.subscriptionsCount)
        mPostsTv.text = resources.getQuantityString(R.plurals.posts, it.postsCount.toInt())
        t.error?.let {
            view?.showSnackbar(it.localizedMessage ?: 0)
        }
        mGolosCountTV.text = "${String.format("%.3f", t?.accountInfo?.golosAmount ?: 0.0)}"
        mUserAvatar.isClickable = mViewModel.canUserSeeVotingPower()
        if (mVotingPowerLo.visibility == View.VISIBLE) {
            val value = it.votingPower / 100.0
            mVotingPowerTv.text = "${String.format("%.2f", value)}%"
        }


        if (mLastAccountInfo?.userCover != t.accountInfo.userCover) {
            t.accountInfo.userCover?.let {
                glide.load(ImageUriResolver.resolveImageWithSize(it))
                        .apply(RequestOptions().centerCrop())
                        .transition(DrawableTransitionOptions
                                .withCrossFade())
                        .into(mUserCoverIv)
            }
            if (t.accountInfo.userCover == null) mUserCoverIv.setImageBitmap(null)
        }

        mLastAccountInfo = t.accountInfo.copy()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        mViewModel.onUserVisibilityChange(isVisibleToUser)
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


