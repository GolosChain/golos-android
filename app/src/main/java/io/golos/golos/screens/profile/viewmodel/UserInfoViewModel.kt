package io.golos.golos.screens.profile.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.InternetStatusNotifier

data class UserAccountModel(val accountInfo: GolosUserAccountInfo,
                            val error: GolosError?,
                            val isActiveUserPage: Boolean,
                            val isFollowButtonVisible: Boolean,
                            val isSubscriptionInProgress: Boolean = false)

class UserInfoViewModel : ViewModel(), Observer<GolosUserAccountInfo> {
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

    }

    private fun showError(error: GolosError) {
        mLiveData.value = UserAccountModel(mLiveData.value?.accountInfo
                ?: GolosUserAccountInfo(GolosUser("")),
                error,
                isActiveUserPage(),
                isFollowButtonVisible(),
                mLiveData.value?.isSubscriptionInProgress ?: false)
    }

    private fun isActiveUserPage() = mRepository.isUserLoggedIn() && mRepository.appUserData.value?.userName == userName

    fun isSettingButtonShown() = isActiveUserPage()

    fun canUserSeeVotingPower() = isActiveUserPage()

    fun isFollowButtonVisible() = mRepository.isUserLoggedIn() && mRepository.appUserData.value?.userName != userName

    fun getLiveData(): LiveData<UserAccountModel> {
        return mLiveData
    }

    fun onUserVisibilityChange(isVisible: Boolean) {
        if (isVisible) mRepository.requestUserInfoUpdate(userName, { _, e ->
            showError(e ?: return@requestUserInfoUpdate)
        }
        )
    }

    override fun onChanged(t: GolosUserAccountInfo?) {
        val accInfo = t ?: GolosUserAccountInfo(GolosUser(""))
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
                    mRepository.unSubscribeOnUserBlog(it.accountInfo.golosUser.userName
                            ?: return@let, resultHandler)
                } else {
                    mRepository.subscribeOnUserBlog(it.accountInfo.golosUser.userName
                            ?: return@let, resultHandler)
                }
            }
        } else {
            showError(GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
        }
    }

    fun onSubscriberClick(ctx: Context, string: String?) {
        UsersListActivity.startForSubscribersOrSubscriptions(ctx, string
                ?: return, ListType.SUBSCRIBERS)
    }

    fun onSubscriptionsClick(ctx: Context, string: String?) {
        UsersListActivity.startForSubscribersOrSubscriptions(ctx, string
                ?: return, ListType.SUBSCRIPTIONS)
    }
}
