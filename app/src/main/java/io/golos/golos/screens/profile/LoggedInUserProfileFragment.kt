package io.golos.golos.screens.profile

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.profile.adapters.MenuAdapter
import io.golos.golos.screens.profile.adapters.MenuItem

/**
 * Created by yuri on 10.11.17.
 */
class LoggedInUserProfileFragment : Fragment() {
    private lateinit var mUserAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mViewModel: AuthViewModel
    private lateinit var mSettingButton:ImageButton
    private lateinit var mMotoTv:TextView
    private lateinit var mSubscribersNumTv:TextView
    private lateinit var mSubscriptionsNum:TextView
    private lateinit var mFragmentContainerVg:ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fr_user_logged_in, container, false)
        mUserAvatar = v.findViewById(R.id.avatar_iv)
        mUserName = v.findViewById(R.id.username_tv)
        mSettingButton = v.findViewById(R.id.settings_btn)
        mMotoTv = v.findViewById(R.id.moto)
        mSubscribersNumTv = v.findViewById(R.id.subscribers_num_tv)
        mSubscriptionsNum = v.findViewById(R.id.subscriptions_num_tv)
        mFragmentContainerVg = v.findViewById(R.id.fragment_container)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        mViewModel.userProfileState.observe(activity as LifecycleOwner, android.arch.lifecycle.Observer {
            mUserName.text = it?.userName?.replace("@", "")?.capitalize()
            val glide = Glide.with(view)
            if (it?.avatarPath == null) glide.load(R.drawable.ic_person_gray_80dp).into(mUserAvatar)
            else {
                glide.load(it.avatarPath)
                        .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_80dp))
                        .error(glide.load(R.drawable.ic_person_gray_80dp))
                        .into(mUserAvatar)
            }
            mUserName.text = "@${it?.userName}"
        })
    }

    companion object {
        fun getInstance(): LoggedInUserProfileFragment {
            val f = LoggedInUserProfileFragment()
            return f
        }
    }
}


