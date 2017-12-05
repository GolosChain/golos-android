package io.golos.golos.screens.profile

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.androidviewmodel.AuthState
import io.golos.golos.screens.androidviewmodel.AuthViewModel
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 05.12.17.
 */
class ProfileRootFragment() : GolosFragment(), Observer<AuthState> {
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
        val authModel = ViewModelProviders.of(this).get(AuthViewModel::class.java)
        authModel.userAuthState.removeObservers(this)
        authModel.userAuthState.observe(this, this)
    }

    override fun onChanged(it: AuthState?) {
        if (it?.isLoggedIn == true) {
            if (childFragmentManager.findFragmentByTag(mUserProfileFTag) == null) {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id, UserProfileDrawerFragment.getInstance(), mUserProfileFTag)
                        .commit()
            }
        } else if (it?.isLoggedIn == false) {
            if (childFragmentManager.findFragmentByTag(mNotLoggedFTag) == null) {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id, NotLoggedInDrawerFragment.getInstance(), mNotLoggedFTag)
                        .commit()
            }
        }
    }
}