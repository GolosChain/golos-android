package io.golos.golos.screens.profile.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.InternetStatusNotifier

data class UserAccountModel(val accountInfo: AccountInfo,
                            val error: GolosError?,
                            val isActiveUserPage: Boolean,
                            val isFollowButtonVisible: Boolean,
                            val isSubscriptionInProgress: Boolean = false)

class UserInfoViewModel : ViewModel(), Observer<AccountInfo> {
    private lateinit var userName: String
    private lateinit var internetStatusNotifier: InternetStatusNotifier
    private val mRepository = Repository.get
    private val mLiveData: MutableLiveData<UserAccountModel> = MutableLiveData()

    fun onCreate(userName: String, internetStatusNotifier: InternetStatusNotifier) {
        this.userName = userName
        this.internetStatusNotifier = internetStatusNotifier
        mRepository
                .getUserInfo(userName)
                .observeForever(this)
        mRepository.requestUserInfoUpdate(userName, { a, e ->
            showError(e ?: return@requestUserInfoUpdate)
        }
        )
    }

    private fun showError(error: GolosError) {
        mLiveData.value = UserAccountModel(mLiveData.value?.accountInfo ?: AccountInfo(""),
                error,
                isActiveUserPage(),
                isFollowButtonVisible(),
                mLiveData.value?.isSubscriptionInProgress ?: false)
    }

    private fun isActiveUserPage() = mRepository.isUserLoggedIn() && mRepository.getCurrentUserDataAsLiveData().value?.userName == userName

    private fun isFollowButtonVisible() = mRepository.isUserLoggedIn() && mRepository.getCurrentUserDataAsLiveData().value?.userName != userName

    fun getLiveData(): LiveData<UserAccountModel> {
        return mLiveData
    }

    override fun onChanged(t: AccountInfo?) {
        val accInfo = t ?: AccountInfo("")
        mLiveData.value = UserAccountModel(accInfo,
                null,
                isActiveUserPage(),
                isFollowButtonVisible(),
                false)
    }

    fun onFollowBtnClick() {
        if (!mRepository.isUserLoggedIn()) return
        if (internetStatusNotifier.isAppOnline()) {
            mLiveData.value?.let {
                mLiveData.value = UserAccountModel(it.accountInfo, null, isActiveUserPage(), isFollowButtonVisible(), true)
                val resultHandler: (Unit, GolosError?) -> Unit = { _, e ->
                    if (e != null) {
                        mLiveData.value = UserAccountModel(it.accountInfo, e, isActiveUserPage(), isFollowButtonVisible(), false)
                    }
                }
                if (it.accountInfo.isCurrentUserSubscribed) {
                    mRepository.unFollow(it.accountInfo.userName ?: return@let, resultHandler)
                } else {
                    mRepository.follow(it.accountInfo.userName ?: return@let, resultHandler)
                }
            }
        } else {
            showError(GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
        }
    }
}
