package io.golos.golos.screens.profile

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.screens.profile.viewmodel.AuthState
import io.golos.golos.screens.profile.viewmodel.AuthViewModel
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 05.12.17.
 */
class ProfileRootFragment : GolosFragment(), Observer<AuthState> {
    private val mNotLoggedFTag = "mNotLoggedFTag"
    private val mUserProfileFTag = "mUserProfileFTag"
    private lateinit var mRoot: ViewGroup
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_profile_root, container, false)
        setUp(v)
        return v
    }


    private fun setUp(view: View) {
        mRoot = view.findViewById(R.id.profile_root)
        val authModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        authModel.userAuthState.observe(this, this)
    }

    override fun onChanged(it: AuthState?) {
        if (it?.isLoggedIn == true) {
            val oldFragment: Fragment? = childFragmentManager.findFragmentByTag(mUserProfileFTag)
            if (oldFragment == null) {
                if (Repository.get.appUserData.value?.name != null)
                    childFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, 0)
                            .replace(mRoot.id,
                                    UserProfileFragment.getInstance(it.username),
                                    mUserProfileFTag)
                            .commit()
            } else {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id,
                                oldFragment,
                                mUserProfileFTag)
                        .commit()
            }
        } else {
            val oldFragment: Fragment? = childFragmentManager.findFragmentByTag(mNotLoggedFTag)
            if (oldFragment == null) {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id, AuthFragment.getInstance(), mNotLoggedFTag)
                        .commit()
            } else {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id, oldFragment, mNotLoggedFTag)
                        .commit()
            }
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isAdded) {
            val fragment = childFragmentManager.findFragmentByTag(mUserProfileFTag)
            if (fragment?.isVisible == true) fragment.userVisibleHint = isVisibleToUser
        }
    }
}