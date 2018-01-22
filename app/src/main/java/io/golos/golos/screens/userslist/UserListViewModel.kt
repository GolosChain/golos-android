package io.golos.golos.screens.userslist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.UserBlogSubscription
import io.golos.golos.repository.model.UserObject
import io.golos.golos.screens.profile.ProfileActivity
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.screens.userslist.model.UserListRowData
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.StringSupplier
import timber.log.Timber

data class UserListViewState(val title: String,
                             val users: List<UserListRowData>,
                             val error: GolosError?)


class UserListViewModel : ViewModel() {
    private var userName: String? = null
    private var storyId: Long? = null
    private lateinit var mListType: ListType
    private lateinit var mStringSupplier: StringSupplier
    private lateinit var mTitle: String
    private val mRepository = Repository.get
    private val mCurrentUsersubscriptions = HashSet<UserBlogSubscription>()
    private lateinit var mInternetStatusNotifier: InternetStatusNotifier

    private val mLiveData = MediatorLiveData<UserListViewState>()

    fun getLiveData(): LiveData<UserListViewState> = mLiveData


    fun onCreate(userName: String?,
                 storyId: Long?,
                 type: ListType,
                 stringSuppliers: StringSupplier,
                 internetStatusNotifier: InternetStatusNotifier) {
        this.userName = userName
        this.mListType = type
        this.mStringSupplier = stringSuppliers
        this.mInternetStatusNotifier = internetStatusNotifier
        this.storyId = storyId

        userName?.let {
            mLiveData.removeSource(mRepository.getSubscriptionsToBlogs(it))
            mLiveData.removeSource(mRepository.getSubscribersToBlog(it))
        }
        storyId?.let {
            mLiveData.removeSource(mRepository.getVotedUsersForDiscussion(it))
        }
        mLiveData.removeSource(mRepository.getCurrentUserSubscriptions())

        mTitle = mStringSupplier.get(if (mListType == ListType.SUBSCRIPTIONS) R.string.subscriptions
        else if (mListType == ListType.SUBSCRIBERS) R.string.subscribers else R.string.voted)

        if (mListType == ListType.SUBSCRIPTIONS || mListType == ListType.SUBSCRIBERS) {
            if (userName == null) {
                Timber.e("for this type $mListType userName must be not null")
                return
            }


            val ld = if (mListType == ListType.SUBSCRIPTIONS) mRepository.getSubscriptionsToBlogs(userName)
            else mRepository.getSubscribersToBlog(userName)
            ld.observeForever({

                mLiveData.value = UserListViewState(
                        mTitle,
                        it?.map {
                            val currentWorkingItem = it
                            UserListRowData(
                                    it.name,
                                    it.avatar,
                                    null,
                                    mCurrentUsersubscriptions.find { it.user.name == currentWorkingItem.name }?.status ?: SubscribeStatus.UnsubscribedStatus)
                        } ?: ArrayList(), null)
            })
            val handler: (List<UserObject>, GolosError?) -> Unit = { _, e ->
                e?.let {
                    mLiveData.value = UserListViewState(mTitle, mLiveData.value?.users ?: ArrayList(), it)
                }
            }
            if (mListType == ListType.SUBSCRIBERS) mRepository.requestSubscribersUpdate(userName, handler)
            else mRepository.requestSubscriptionUpdate(userName, handler)
        } else {
            if (storyId == null) {
                Timber.e("for this type $mListType storyId must be not null")
                return
            }
            mLiveData.addSource(mRepository.getVotedUsersForDiscussion(storyId), {

                if (it == null) {
                    mLiveData.value = null
                    return@addSource
                }
                mLiveData.value = UserListViewState(mTitle, it.map {
                    val currentVotingObject = it
                    val excheangeCourse = mRepository.getExchengeLiveData()
                    UserListRowData(it.name,
                            it.avatar,
                            "$ ${String.format("%.3f", it.gbgValue * (excheangeCourse.value?.dollarsPerGbg ?: 1.0))}",
                            mCurrentUsersubscriptions.find { it.user.name == currentVotingObject.name }?.status ?: SubscribeStatus.UnsubscribedStatus)
                }
                        , null)
            })

        }

        mLiveData.addSource(mRepository.getCurrentUserSubscriptions(), {
            mCurrentUsersubscriptions.clear()
            mCurrentUsersubscriptions.addAll(it ?: listOf())
            if (mLiveData.value == null) return@addSource
            mLiveData.value = UserListViewState(mTitle,
                    (mLiveData.value?.users ?: listOf()).onEach {
                        val currentWorkingitem = it
                        val item = mCurrentUsersubscriptions.find { it.user.name == currentWorkingitem.name }
                        if (item != null) {
                            currentWorkingitem.subscribeStatus = item.status
                        } else {
                            currentWorkingitem.subscribeStatus = SubscribeStatus.UnsubscribedStatus
                        }
                    }, null)
        })
    }

    fun onUserClick(ctx: Context, it: UserListRowData) {
        ProfileActivity.start(ctx, it.name)
    }

    fun onSubscribeClick(it: UserListRowData) {
        if (!mInternetStatusNotifier.isAppOnline()) {
            mLiveData.value = UserListViewState(mTitle, mLiveData.value?.users ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
        } else if (!Repository.get.isUserLoggedIn()) {
            mLiveData.value = UserListViewState(mTitle, mLiveData.value?.users ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))

        } else {
            val handler: (Unit, GolosError?) -> Unit = { _, e ->
                if (e != null) mLiveData.value = UserListViewState(mTitle, mLiveData.value?.users ?: ArrayList(), e)
            }
            if (it.subscribeStatus.isCurrentUserSubscribed) Repository.get.unSubscribeOnUserBlog(it.name, handler)
            else Repository.get.subscribeOnUserBlog(it.name, handler)
        }
    }

    fun onDestroy() {
        userName?.let {
            mLiveData.removeSource(mRepository.getSubscriptionsToBlogs(it))
            mLiveData.removeSource(mRepository.getSubscribersToBlog(it))
        }
        storyId?.let {
            mLiveData.removeSource(mRepository.getVotedUsersForDiscussion(it))
        }
        mLiveData.removeSource(mRepository.getCurrentUserSubscriptions())
    }
}