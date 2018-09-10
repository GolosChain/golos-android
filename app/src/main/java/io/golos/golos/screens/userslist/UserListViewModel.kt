package io.golos.golos.screens.userslist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.ApplicationUser
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.screens.userslist.model.UserListRowData
import io.golos.golos.utils.*
import timber.log.Timber

data class UserListViewState(val title: String,
                             val users: List<UserListRowData>,
                             val error: GolosError?)


class UserListViewModel : ViewModel() {
    private var userName: String? = null
    private var storyId: Long? = null
    private lateinit var mListType: ListType
    private lateinit var mStringSupplier: StringProvider
    private lateinit var mTitle: String
    private val mRepository = Repository.get
    // private val mCurrentUserSubscriptions = HashSet<UserBlogSubscription>()
    private lateinit var mInternetStatusNotifier: InternetStatusNotifier

    private val mLiveData = MediatorLiveData<UserListViewState>()

    fun getLiveData(): LiveData<UserListViewState> = mLiveData
    private var mLastUserName: String? = null
    private var isSubscribed = false

    private val mUserDataObserver = Observer<ApplicationUser> {
        if (it?.isLogged == true) {
            if (!isSubscribed) {
                val userName = it.name
                mLastUserName = userName
                mLiveData.addSource(mRepository.getGolosUserSubscriptions(userName)) {
                    onValueChanged()
                }
                mLiveData.addSource(mRepository.currentUserSubscriptionsUpdateStatus) { onValueChanged() }
                isSubscribed = true
            }

        } else {
            isSubscribed = false
            if (mLastUserName != null) {
                mLiveData.removeSource(mRepository.getGolosUserSubscriptions(mLastUserName
                        ?: return@Observer))
                mLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
            }
        }
    }


    fun onStart() {


        mRepository.appUserData.observeForever(mUserDataObserver)
        if (mListType == ListType.SUBSCRIPTIONS || mListType == ListType.SUBSCRIBERS) {
            if (userName == null) {
                Timber.e("for this type $mListType userName must be not null")
                return
            }
            val ld = if (mListType == ListType.SUBSCRIPTIONS) mRepository.getGolosUserSubscriptions(userName
                    ?: return)
            else mRepository.getGolosUserSubscribers(userName ?: return)

            if (mListType == ListType.SUBSCRIPTIONS && userName == mRepository.appUserData.value?.name) {//if it is subscriptions for current users - we already subscribed


            }else {
                Timber.e("adding source")
                mLiveData.addSource(ld) { onValueChanged() }
            }


        } else {
            if (storyId == null) {
                Timber.e("for this type $mListType storyId must be not null")
                return
            }
            mLiveData.addSource(mRepository.getVotedUsersForDiscussion(storyId ?: return)) {
                if (it == null) {
                    mLiveData.value = null
                    return@addSource
                }
                onValueChanged()
            }
        }
    }

    fun onStop() {

        userName?.let {
            mLiveData.removeSource(mRepository.getGolosUserSubscriptions(it))
            mLiveData.removeSource(mRepository.getGolosUserSubscribers(it))
        }
        storyId?.let {
            mLiveData.removeSource(mRepository.getVotedUsersForDiscussion(it))
        }

        mRepository.appUserData.removeObserver(mUserDataObserver)
        mLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
    }

    private fun onValueChanged() {
        val userAvatars = mRepository.usersAvatars.value.orEmpty()
        val currentUserSubscriptions =
                (if (mRepository.appUserData.value?.isLogged == true)
                    mRepository.getGolosUserSubscriptions(mRepository.appUserData.value?.name.orEmpty()).value.orEmpty()
                else emptyList()).toSet()

        val updatingStates = mRepository.currentUserSubscriptionsUpdateStatus.value.orEmpty()

        if (mListType == ListType.SUBSCRIPTIONS || mListType == ListType.SUBSCRIBERS) {
            val userName = userName ?: return
            val users = if (mListType == ListType.SUBSCRIPTIONS) mRepository.getGolosUserSubscriptions(userName)
            else mRepository.getGolosUserSubscribers(userName)
            if (users.value == null) {

                mLiveData.value = null
            } else {
                mLiveData.value = UserListViewState(mTitle,
                        users.value
                                .orEmpty()
                                .map {
                                    UserListRowData(it,
                                            userAvatars[it],
                                            null,
                                            SubscribeStatus.create(currentUserSubscriptions.contains(it),
                                                    updatingStates[it] ?: UpdatingState.DONE))
                                },
                        null)
            }


        } else {
            val exchangeValues = mRepository.getExchangeLiveData().value

            val chosenCurrency = mRepository.userSettingsRepository.getCurrency().value
                    ?: UserSettingsRepository.GolosCurrency.USD

            val votes = mRepository.getVotedUsersForDiscussion(storyId ?: return).value.orEmpty()

            mLiveData.value = UserListViewState(mTitle,
                    votes.map {

                        val gbgCost = it.gbgValue
                        val displayFormatter = Repository.get.userSettingsRepository.getBountDisplay().value
                                ?: UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
                        val outString = if (exchangeValues == null) {
                            mStringSupplier.get(R.string.gbg_format, displayFormatter.formatNumber(gbgCost))
                        } else when (chosenCurrency) {
                            UserSettingsRepository.GolosCurrency.RUB -> mStringSupplier.get(R.string.rubles_format, displayFormatter.formatNumber(gbgCost
                                    * exchangeValues.rublesPerGbg))
                            UserSettingsRepository.GolosCurrency.GBG -> mStringSupplier.get(R.string.gbg_format, displayFormatter.formatNumber(gbgCost))
                            else -> mStringSupplier.get(R.string.dollars_format, displayFormatter.formatNumber(gbgCost
                                    * exchangeValues.dollarsPerGbg))
                        }
                        UserListRowData(it.name,
                                userAvatars[it.name],
                                outString,
                                SubscribeStatus.create(currentUserSubscriptions.contains(it.name),
                                        updatingStates[it.name] ?: UpdatingState.DONE))
                    }
                    , null)

        }
        mLiveData.value?.let {
            val usesWithNoAvatars = it.users.filter {
                it.avatar == null
                        && !mRepository.usersAvatars.value.orEmpty().containsKey(it.name)
            }
                    .map { it.name }
            mRepository.requestUsersAccountInfoUpdate(usesWithNoAvatars)
        }
    }


    fun onCreate(userName: String?,
                 storyId: Long?,
                 type: ListType,
                 stringSuppliers: StringProvider,
                 internetStatusNotifier: InternetStatusNotifier) {
        this.userName = userName
        this.mListType = type
        this.mStringSupplier = stringSuppliers
        this.mInternetStatusNotifier = internetStatusNotifier
        this.storyId = storyId

        mTitle = mStringSupplier.get(when (mListType) {
            ListType.SUBSCRIPTIONS -> R.string.subscriptions
            ListType.SUBSCRIBERS -> R.string.subscribers
            else -> R.string.voted
        }, null)

        if (mListType == ListType.SUBSCRIBERS) mRepository.requestGolosUserSubscribersUpdate(userName.orEmpty())
        else if (mListType == ListType.SUBSCRIPTIONS) mRepository.requestGolosUserSubscriptionsUpdate(userName.orEmpty())

    }

    fun onUserClick(ctx: Context, it: UserListRowData) {
        UserProfileActivity.start(ctx, it.name)
    }

    fun onSubscribeClick(it: UserListRowData) {
        if (!mInternetStatusNotifier.isAppOnline()) {
            mLiveData.value = mLiveData.value?.copy(error = GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
        } else if (!Repository.get.isUserLoggedIn()) {
            mLiveData.value = mLiveData.value?.copy(error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))

        } else {
            val handler: (Unit, GolosError?) -> Unit = { _, e ->
                if (e != null) mLiveData.value = mLiveData.value?.copy(error = e)
            }
            if (it.subscribeStatus?.isCurrentUserSubscribed == true) Repository.get.unSubscribeFromGolosUserBlog(it.name, handler)
            else if (it.subscribeStatus?.isCurrentUserSubscribed == false) Repository.get.subscribeOnGolosUserBlog(it.name, handler)
        }
    }
}