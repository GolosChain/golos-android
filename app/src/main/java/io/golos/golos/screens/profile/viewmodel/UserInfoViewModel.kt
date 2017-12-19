package io.golos.golos.screens.profile.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 18.12.17.
 */
data class UserAccountModel(val accountInfo: AccountInfo,
                            val error: GolosError?)

class UserInfoViewModel : ViewModel(), Observer<AccountInfo> {
    private lateinit var userName: String
    private val mRepos = Repository.get
    private val mLiveData: MutableLiveData<UserAccountModel> = MutableLiveData()

    fun onCreate(userName: String) {
        this.userName = userName
        mRepos.getUserInfo(userName).observeForever(this)
        mRepos.requestUserInfoUpdate(userName, { a, e ->
            if (e != null) {
                mLiveData.value = UserAccountModel(mLiveData.value?.accountInfo ?: AccountInfo(""), e)
            }
        }
        )
    }

    fun getLiveData(): LiveData<UserAccountModel> {
        return mLiveData
    }

    override fun onChanged(t: AccountInfo?) {
        mLiveData.value = UserAccountModel(t ?: AccountInfo(""), null)
    }
}