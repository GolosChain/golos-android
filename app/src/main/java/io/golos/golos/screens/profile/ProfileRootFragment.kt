package io.golos.golos.screens.profile

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.Repository
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
        val authModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        val owner = activity as LifecycleOwner
        authModel.userAuthState.observe(owner, this)
    }

    override fun onChanged(it: AuthState?) {
        if (it?.isLoggedIn == true) {
            if (Repository.get.getCurrentUserDataAsLiveData().value?.userName != null)
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id,
                                LoggedInUserProfileFragment.getInstance(it.username),
                                mUserProfileFTag)
                        .commit()
        } else {
            if (childFragmentManager.findFragmentByTag(mNotLoggedFTag) == null) {
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, 0)
                        .replace(mRoot.id, NotLoggedInFragment.getInstance(), mNotLoggedFTag)
                        .commit()
            }
        }
    }
}