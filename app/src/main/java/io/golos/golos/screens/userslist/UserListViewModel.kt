package io.golos.golos.screens.userslist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.FollowUserObject
import io.golos.golos.screens.profile.ProfileActivity
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError

data class UserListViewState(val users: List<FollowUserObject>,
                             val error: GolosError?)

class UserListViewModel : ViewModel() {
    private lateinit var userName: String
    private var subscribersOrSubscriptions: Boolean = false

    private val mLiveData = MutableLiveData<UserListViewState>()

    fun getLiveData(): LiveData<UserListViewState> = mLiveData


    fun onCreate(userName: String,
                 subscribersOrSubscriptions: Boolean) {
        this.userName = userName
        this.subscribersOrSubscriptions = subscribersOrSubscriptions
        val ld = if (subscribersOrSubscriptions) Repository.get.getSubscribersToUserBlog(userName)
        else Repository.get.getSubscriptionsToUserBlogs(userName)
        ld.observeForever({
            mLiveData.value = UserListViewState(it ?: ArrayList(), null)
        })
        val handler: (List<FollowUserObject>, GolosError?) -> Unit = { _, e ->
            e?.let {
                mLiveData.value = UserListViewState(mLiveData.value?.users ?: ArrayList(), it)
            }
        }
        if (subscribersOrSubscriptions) Repository.get.requestSubscribersUpdate(userName, handler)
        else Repository.get.requestSubscriptionUpdate(userName, handler)
    }

    fun onUserClick(ctx: Context, it: FollowUserObject) {
        ProfileActivity.start(ctx, it.name)
    }

    fun onSubscribeClick(ctx: Context, it: FollowUserObject) {
        if (!App.isAppOnline()) {
            mLiveData.value = UserListViewState(mLiveData.value?.users ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
        } else if (!Repository.get.isUserLoggedIn()) {
            mLiveData.value = UserListViewState(mLiveData.value?.users ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))

        } else {
            if (it.subscribeStatus.isCurrentUserSubscribed) {
                val handler: (Unit, GolosError?) -> Unit = { _, e ->
                    if (e != null) mLiveData.value = UserListViewState(mLiveData.value?.users ?: ArrayList(), e)
                }
                if (it.subscribeStatus.isCurrentUserSubscribed) Repository.get.unFollow(it.name, handler)
                else Repository.get.follow(it.name, handler)
            }
        }
    }
}