package io.golos.golos.screens.profile.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ApplicationUser
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.UpdatingState
import timber.log.Timber

data class UserAccountModel(val accountInfo: GolosUserAccountInfo,
                            val followButtonText: Int,
                            val error: GolosError?,
                            val isActiveUserPage: Boolean,
                            val isFollowButtonVisible: Boolean,
                            val isSubscriptionInProgress: Boolean = false)

class UserInfoViewModel : ViewModel(), Observer<ApplicationUser> {
    private var mLastUserName: String? = null
    private var mLstCurrentUserHash: Int = 0
    override fun onChanged(t: ApplicationUser?) {
        if (t?.isLogged == true) {
            mLastUserName = t.name
            mLiveData.addSource(mRepository.getGolosUserSubscriptions(mLastUserName
                    ?: return)) {

                onChanged()
            }
            mLiveData.addSource(mRepository.currentUserSubscriptionsUpdateStatus) { onChanged() }
        } else {
            mLiveData.removeSource(mRepository.getGolosUserSubscriptions(mLastUserName ?: return))
            mLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
        }
    }

    private lateinit var userName: String
    private lateinit var internetStatusNotifier: InternetStatusNotifier
    private val mRepository = Repository.get
    private val mLiveData: MediatorLiveData<UserAccountModel> = MediatorLiveData()

    fun onCreate(userName: String, internetStatusNotifier: InternetStatusNotifier) {
        this.userName = userName
        this.internetStatusNotifier = internetStatusNotifier

    }

    fun onStart() {
        mLiveData.addSource(mRepository
                .getGolosUserAccountInfos()) {
            val myUser = it?.get(userName)?:return@addSource

            if (myUser.hashCode() != mLstCurrentUserHash){
                onChanged()
                mLstCurrentUserHash = myUser.hashCode()
            }
        }
        mRepository.appUserData.observeForever(this)
    }

    fun onStop() {
        mLiveData.removeSource(mRepository
                .getGolosUserAccountInfos())
        mRepository.appUserData.removeObserver(this)
        mLiveData.removeSource(mRepository.getGolosUserSubscriptions(mLastUserName ?: return))
        mLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
    }

    private fun showError(error: GolosError) {
        mLiveData.value = mLiveData.value?.copy(error = error)
    }

    private fun isActiveUserPage() = mRepository.isUserLoggedIn() && mRepository.appUserData.value?.name.orEmpty() == userName

    fun isSettingButtonShown() = isActiveUserPage()

    fun canUserSeeVotingPower() = isActiveUserPage()

    fun isFollowButtonVisible() = mRepository.isUserLoggedIn() && mRepository.appUserData.value?.name.orEmpty() != userName

    fun getLiveData(): LiveData<UserAccountModel> {
        return mLiveData
    }

    fun onUserVisibilityChange(isVisible: Boolean) {
        if (isVisible) mRepository.requestUsersAccountInfoUpdate(listOf(userName)
        ) { _, e ->
            showError(e ?: return@requestUsersAccountInfoUpdate)
        }
    }

    private fun onChanged() {
        Timber.e("onChanged $userName")
        val isUserLogged = mRepository.appUserData.value?.isLogged ?: false
        val currentUserName = mRepository.appUserData.value?.name.orEmpty()
        val usersData = mRepository.getGolosUserAccountInfos().value.orEmpty()[userName] ?: return
        val currentUserSubscriptions =
                if (isUserLogged) mRepository.getGolosUserSubscriptions(currentUserName).value.orEmpty() else emptyList<String>().toSet()
        val updatingStates = mRepository.currentUserSubscriptionsUpdateStatus.value.orEmpty()

        mLiveData.value = UserAccountModel(usersData,
                if (currentUserSubscriptions.contains(userName)) R.string.unfollow else R.string.follow,
                null,
                isActiveUserPage(),
                isFollowButtonVisible(),
                updatingStates[userName] == UpdatingState.UPDATING)
    }

    fun onFollowBtnClick() {
        if (!mRepository.isUserLoggedIn()) return
        if (internetStatusNotifier.isAppOnline()) {
            mLiveData.value?.let {
                mLiveData.value = mLiveData.value?.copy(isSubscriptionInProgress = true)
                val resultHandler: (Unit, GolosError?) -> Unit = { _, e ->
                    if (e != null) {
                        mLiveData.value = mLiveData.value?.copy(isSubscriptionInProgress = false, error = e)
                    }
                }
                val isUserLogged = mRepository.appUserData.value?.isLogged ?: false
                val currentUserName = mRepository.appUserData.value?.name.orEmpty()
                val currentUserSubscriptions =
                        if (isUserLogged) mRepository.getGolosUserSubscriptions(currentUserName).value.orEmpty() else emptyList<String>().toSet()

                if (currentUserSubscriptions.contains(userName)) {
                    mRepository.unSubscribeFromGolosUserBlog(userName, resultHandler)
                } else {
                    mRepository.subscribeOnGolosUserBlog(userName, resultHandler)
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
