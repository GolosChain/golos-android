package io.golos.golos.screens.profile

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.profile.viewmodel.UserAccountModel
import io.golos.golos.screens.profile.viewmodel.UserInfoViewModel
import io.golos.golos.screens.settings.SettingsActivity
import io.golos.golos.utils.*
import timber.log.Timber

class WalletActivity : GolosActivity(), Observer<UserAccountModel> {
    private lateinit var mUserAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mSettingButton: ImageButton
    private lateinit var mAvatarOverlay: View
    private lateinit var mVotingPowerLo: View
    private lateinit var mVotingPowerTv: TextView
    private lateinit var mVotingPowerIndicator: ProgressBar
    private lateinit var mViewModel: UserInfoViewModel
    private lateinit var mUserCoverIv: ImageView

    override fun onChanged(t: UserAccountModel?) {
        val it = t?.accountInfo ?: return

        mUserName.text = it.userName.capitalize()
        val glide = Glide.with(this)
        if (it.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
        else {
            glide.load(ImageUriResolver.resolveImageWithSize(it.avatarPath, wantedwidth = mUserAvatar.width))
                    .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                    .error(glide.load(R.drawable.ic_person_gray_80dp))
                    .into(mUserAvatar)
        }
        if (mVotingPowerLo.visibility == View.VISIBLE) {
            val value = it.votingPower / 100.0
            mVotingPowerTv.text = "${String.format("%.2f", value)}%"
        }
        if (it.userCover != null) {
            glide.load(ImageUriResolver.resolveImageWithSize(it.userCover)).apply(RequestOptions().centerCrop()).into(mUserCoverIv)
            mUserName.setTextColor(getColorCompat(android.R.color.white))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_wallet)
        mViewModel = ViewModelProviders.of(this).get(UserInfoViewModel::class.java)
        setUpViews()

        mViewModel.getLiveData().observe(this, this)
        val userName = intent.getStringExtra("userName") ?: return

        mViewModel.onCreate(userName, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return App.isAppOnline()
            }
        })

        val fragment = WalletFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragments_frame,
                fragment).commit()

    }

    private fun setUpViews() {
        mUserAvatar = findViewById(R.id.avatar_iv)
        mUserName = findViewById(R.id.username_tv)
        mSettingButton = findViewById(R.id.settings_btn)
        mAvatarOverlay = findViewById(R.id.avatar_overlay)
        mVotingPowerLo = findViewById(R.id.voting_power_lo)
        mVotingPowerTv = findViewById(R.id.voting_power_tv)
        mVotingPowerIndicator = findViewById(R.id.voting_power_progress)
        mUserCoverIv = findViewById(R.id.cover_iv)
        mVotingPowerIndicator.setOnClickListener {
            mAvatarOverlay.setViewGone()
            mVotingPowerIndicator.setViewGone()
            mVotingPowerLo.setViewGone()
        }


        mSettingButton.setOnClickListener {
            val i = Intent(this, SettingsActivity::class.java)
            startActivityForResult(i, CHANGE_THEME)
        }
        findViewById<View>(R.id.back_btn).setOnClickListener { onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        mViewModel.onStart()
        if (mViewModel.canUserSeeVotingPower()) {
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
        }
        if (mViewModel.isSettingButtonShown()) mSettingButton.setViewVisible()
        else mSettingButton.setViewGone()
    }

    override fun onStop() {
        super.onStop()
        mViewModel.onStop()
    }

    companion object {
        fun start(fromActivity: Activity, userName: String) {
            val i = Intent(fromActivity, WalletActivity::class.java)
            Timber.e("start $userName")
            i.putExtra("userName", userName)
            fromActivity.startActivity(i)
        }
    }

}
