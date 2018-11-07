package io.golos.golos.utils

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import io.golos.golos.repository.Repository
import java.lang.ref.WeakReference

/**
 * Created by yuri yurivladdurain@gmail.com on 24/10/2018.
 */
class RepostObserver(fragmentManager: FragmentManager) : Observer<Unit> {
    private val mFragmentManager: WeakReference<FragmentManager> = WeakReference(fragmentManager)
    override fun onChanged(t: Unit?) {
        if (t == null) return
        if (Repository.get.lastRepost.value == null) return
        ReblogSuccesDialog.getInstance().show(mFragmentManager.get() ?: return, null)
    }
}