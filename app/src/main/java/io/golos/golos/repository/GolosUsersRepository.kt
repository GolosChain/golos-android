package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.support.annotation.WorkerThread
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.TreeSet
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.collections.set

interface GolosUsersRepository {

    fun getGolosUserAccountInfos(): LiveData<Map<String, GolosUserAccountInfo>>

    fun requestUsersAccountInfoUpdate(golosUserName: List<String>,
                                      completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun getGolosUserSubscribers(golosUserName: String): LiveData<List<String>>

    fun requestGolosUserSubscribersUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun getGolosUserSubscriptions(golosUserName: String): LiveData<List<String>>

    fun requestGolosUserSubscriptionsUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun subscribeOnGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun unSubscribeFromGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    fun addAccountInfo(list: List<GolosUserAccountInfo>)

    val currentUserSubscriptionsUpdateStatus: LiveData<Map<String, UpdatingState>>


    val usersAvatars: LiveData<Map<String, String?>>

    val currentUserSubscriptions: LiveData<List<String>>

    fun lookupUsers(username: String): LiveData<List<String>>


}

interface GolosUsersPersister {

    fun saveGolosUsersAccountInfo(list: List<GolosUserAccountInfo>)

    fun saveGolosUsersSubscribers(map: Map<String, List<String>>)

    fun saveGolosUsersSubscriptions(map: Map<String, List<String>>)

    fun getGolosUsersAccountInfo(): List<GolosUserAccountInfo>

    fun getGolosUsersSubscribers(): Map<String, List<String>>

    fun getGolosUsersSubscriptions(): Map<String, List<String>>

}

interface GolosUsersApi {

    fun getGolosUsers(names: List<String>, fetchSubsInfo: Boolean): Map<String, GolosUserAccountInfo>

    fun getGolosUserSubscriptions(forUser: String, startFrom: String?): List<String>

    fun getGolosUserSubscribers(forUser: String, startFrom: String?): List<String>

    fun subscribe(onUser: String)

    fun unSubscribe(fromUser: String)

    fun lookUpUsers(nick: String): List<String>
}

class UsersRepositoryImpl(private val mPersister: GolosUsersPersister,
                          private val mCurrentUserInfo: UserDataProvider,
                          private val mGolosApi: GolosUsersApi,
                          private val mWorkerExecutor: Executor = Executors.newSingleThreadExecutor(),
                          private val mMainThreadExecutor: Executor = MainThreadExecutor(),
                          private val mUserInfoRefreshDelay: Long = TimeUnit.DAYS.toMillis(5),
                          private val mLogger: ExceptionLogger = FabricExceptionLogger) : GolosUsersRepository {


    private val mGolosUsers: MutableLiveData<Map<String, GolosUserAccountInfo>> = MutableLiveData()
    private val mUsersSubscriptions: HashMap<String, MutableLiveData<List<String>>> = HashMap()
    private val mUsersSubscribers: HashMap<String, MutableLiveData<List<String>>> = HashMap()
    private val mUpdatingStates = MutableLiveData<HashMap<String, UpdatingState>>()
    private val mUsersName = TreeSet<String>()
    private val mCurrentUserSubscriptions = MediatorLiveData<List<String>>()

    private var isSubscribedOnCurrentUserSubscriptions = false
    private var mLastName: String? = null


    override fun getGolosUserAccountInfos(): LiveData<Map<String, GolosUserAccountInfo>> {
        return mGolosUsers
    }

    override val currentUserSubscriptions = mCurrentUserSubscriptions


    override val usersAvatars: LiveData<Map<String, String?>> = Transformations.map(mGolosUsers) {

        it.mapValues { it.value.avatarPath }
    }

    override fun requestUsersAccountInfoUpdate(golosUserName: List<String>,
                                               completionHandler: (Unit, GolosError?) -> Unit) {

        if (golosUserName.isEmpty()) {
            completionHandler(Unit, null)
            return
        }
        mWorkerExecutor.execute {
            try {
                val users =
                        mGolosApi.getGolosUsers(golosUserName.distinct(), golosUserName.size == 1).toHashMap()
                val allSaveUsers = mGolosUsers.value.orEmpty().toHashMap()

                if (golosUserName.size != 1) {
                    val allSavedSubscriptionsData = allSaveUsers.mapValues { Pair(it.value.subscribersCount, it.value.subscriptionsCount) }
                    allSavedSubscriptionsData.forEach {
                        val newInfoWithOldSubscribers = users[it.key]?.copy(subscribersCount = it.value.first, subscriptionsCount = it.value.second)
                        if (newInfoWithOldSubscribers != null)
                            users[it.key] = newInfoWithOldSubscribers
                    }
                }

                allSaveUsers.putAll(users)
                mMainThreadExecutor.execute {
                    mGolosUsers.value = allSaveUsers
                }
                mPersister.saveGolosUsersAccountInfo(allSaveUsers.map { it.value })
            } catch (e: Exception) {
                mLogger.log(e)
                mMainThreadExecutor.execute {
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun lookupUsers(username: String): LiveData<List<String>> {
        val liveData = MutableLiveData<List<String>>()
        mWorkerExecutor.execute {

            var users = mUsersName.filter {
                it.startsWith(username)
            }

            mMainThreadExecutor.execute {
                liveData.value = users
            }
            val foundUsers = mGolosApi.lookUpUsers(username)

            mUsersName.addAll(foundUsers)
            users = mUsersName.filter {

                it.startsWith(username)
            }
            mMainThreadExecutor.execute {
                liveData.value = users

            }
        }
        return liveData
    }

    override fun addAccountInfo(list: List<GolosUserAccountInfo>) {
        if (list.isEmpty()) return
        mMainThreadExecutor.execute {
            val new = list.associateBy { it.userName }
            mGolosUsers.value = new + mGolosUsers.value.orEmpty()
        }
        mWorkerExecutor.execute { mPersister.saveGolosUsersAccountInfo(list) }
    }

    override fun requestGolosUserSubscribersUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit) {
        if (!mUsersSubscribers.containsKey(golosUserName)) mUsersSubscribers[golosUserName] = MutableLiveData()
        mWorkerExecutor.execute {
            try {
                val subscribers = mGolosApi.getGolosUserSubscribers(golosUserName, null).sortedBy { it[0] }
                mMainThreadExecutor.execute {
                    mUsersSubscribers[golosUserName]?.value = subscribers
                    completionHandler(Unit, null)
                }
                mPersister.saveGolosUsersSubscribers(mapOf(golosUserName toN subscribers))
            } catch (e: Exception) {
                mLogger.log(e)
                mMainThreadExecutor.execute { completionHandler(Unit, GolosErrorParser.parse(e)) }
            }
        }
    }

    @WorkerThread
    private fun isUserSubscribedOn(onAuthor: String): Boolean {
        if (mCurrentUserInfo.appUserData.value?.isLogged == true) {
            val userName = mCurrentUserInfo.appUserData.value?.name.orEmpty()
            var subs = getGolosUserSubscriptions(userName).value

            if (subs.isNullOrEmpty()) {
                subs = mGolosApi
                        .getGolosUserSubscriptions(userName, null)

                mMainThreadExecutor.execute {
                    mUsersSubscriptions[userName]?.value = subs
                }
            }
            return subs.orEmpty().contains(onAuthor)
        }
        return false
    }

    override val currentUserSubscriptionsUpdateStatus
        get() = mUpdatingStates as LiveData<Map<String, UpdatingState>>

    private fun followOrUnfollow(isFollow: Boolean,
                                 user: String,
                                 completionHandler: (Unit, GolosError?) -> Unit) {
        val isUserLoggedIn = mCurrentUserInfo.appUserData.value?.isLogged == true
        val currentUserName = mCurrentUserInfo.appUserData.value?.name.orEmpty()

        if (!isUserLoggedIn) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                R.string.must_be_logged_in_for_this_action))
            }
            return
        }
        if (isUserLoggedIn && currentUserName == user) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                R.string.you_cannot_subscribe_on_yourself))
            }
            return
        }
        if ((isFollow && isUserSubscribedOn(user)) || (!isFollow && !isUserSubscribedOn(user))) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                if (isFollow) R.string.you_already_subscribed else R.string.must_be_subscribed_for_action))
            }
            return
        }
        Timber.e("subscribin")

        val updatingStates = mUpdatingStates.value ?: hashMapOf()
        updatingStates[user] = UpdatingState.UPDATING
        mUpdatingStates.value = updatingStates

        mWorkerExecutor.execute {
            try {
                if (isFollow) mGolosApi.subscribe(user) else mGolosApi.unSubscribe(user)

                val allCurentUserSubscroptions = mUsersSubscriptions[currentUserName]?.value.orEmpty()

                val allUserData = mGolosUsers.value.orEmpty().toHashMap()
                val currentUserData = allUserData[currentUserName]

                mMainThreadExecutor.execute {
                    if (isFollow) {
                        mUsersSubscriptions[currentUserName]?.value = listOf(user) + allCurentUserSubscroptions
                        currentUserData?.let {
                            allUserData.put(it.userName, it.copy(subscriptionsCount = it.subscriptionsCount + 1))
                        }
                    } else {
                        mUsersSubscriptions[currentUserName]?.value = allCurentUserSubscroptions - listOf(user)
                        currentUserData?.let {
                            allUserData.put(it.userName, it.copy(subscriptionsCount = it.subscriptionsCount - 1))
                        }
                    }
                    requestUsersAccountInfoUpdate(listOf(user))
                    updatingStates[user] = UpdatingState.DONE
                    mUpdatingStates.value = updatingStates
                    completionHandler.invoke(Unit, null)
                }


            } catch (e: Exception) {
                FabricExceptionLogger.log(e)
                mMainThreadExecutor.execute {
                    updatingStates[user] = UpdatingState.FAILED
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun subscribeOnGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        followOrUnfollow(true, user, completionHandler)
    }


    override fun unSubscribeFromGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        followOrUnfollow(false, user, completionHandler)
    }

    override fun getGolosUserSubscribers(golosUserName: String): LiveData<List<String>> {
        if (!mUsersSubscribers.containsKey(golosUserName)) {
            mUsersSubscribers[golosUserName] = MutableLiveData()
        }

        return mUsersSubscribers[golosUserName]!!
    }

    override fun getGolosUserSubscriptions(golosUserName: String): LiveData<List<String>> {
        if (!mUsersSubscriptions.containsKey(golosUserName)) {
            mUsersSubscriptions[golosUserName] = MutableLiveData()
        }

        return mUsersSubscriptions[golosUserName]!!
    }

    override fun requestGolosUserSubscriptionsUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit) {
        if (!mUsersSubscriptions.containsKey(golosUserName)) mUsersSubscriptions[golosUserName] = MutableLiveData()
        mWorkerExecutor.execute {
            try {
                val subscriptions = mGolosApi.getGolosUserSubscriptions(golosUserName, null).sortedBy { it[0] }
                mMainThreadExecutor.execute {
                    mUsersSubscriptions[golosUserName]?.value = subscriptions
                    completionHandler(Unit, null)
                }
                mPersister.saveGolosUsersSubscriptions(mapOf(golosUserName toN subscriptions))
            } catch (e: Exception) {
                mLogger.log(e)
                mMainThreadExecutor.execute { completionHandler(Unit, GolosErrorParser.parse(e)) }
            }
        }
    }


    fun setUp() {
        mWorkerExecutor.execute {
            addAccountInfo(mPersister.getGolosUsersAccountInfo())
            addSubscriptions(mPersister.getGolosUsersSubscriptions())
            addSubscribers(mPersister.getGolosUsersSubscribers())
        }
        mCurrentUserSubscriptions.addSource(mCurrentUserInfo.appUserData) {

            if (it?.isLogged == true) {
                if (!isSubscribedOnCurrentUserSubscriptions) {
                    mLastName = it.name
                    mCurrentUserSubscriptions.addSource(getGolosUserSubscriptions(mLastName.orEmpty())) {
                        val subscribers = it.orEmpty()
                        val oldValue = mCurrentUserSubscriptions.value.orEmpty()
                        if (!subscribers.compareContents(oldValue)) mCurrentUserSubscriptions.value = subscribers
                    }

                    isSubscribedOnCurrentUserSubscriptions = true
                }
            } else {
                isSubscribedOnCurrentUserSubscriptions = false
                mCurrentUserSubscriptions.removeSource(getGolosUserSubscriptions(mLastName.orEmpty()))
                if (mCurrentUserSubscriptions.value != null) mCurrentUserSubscriptions.value = null
            }
        }
    }

    private fun addSubscribers(golosUsersSubscribers: Map<String, List<String>>) {
        mMainThreadExecutor.execute {
            golosUsersSubscribers.forEach {
                if (!mUsersSubscribers.contains(it.key)) mUsersSubscribers[it.key] = MutableLiveData()
                mUsersSubscribers[it.key]?.value = it.value.sortedBy { it[0] }
            }
        }
    }

    private fun addSubscriptions(golosUsersSubscriptions: Map<String, List<String>>) {
        mMainThreadExecutor.execute {
            golosUsersSubscriptions.forEach {
                if (!mUsersSubscriptions.contains(it.key)) mUsersSubscriptions[it.key] = MutableLiveData()
                mUsersSubscriptions[it.key]?.value = it.value.sortedBy { it[0] }
            }
        }
    }
}