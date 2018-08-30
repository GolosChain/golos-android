package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.WorkerThread
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.utils.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

interface GolosUsersRepository {

    fun getGolosUserAccountInfos(names: List<String>): List<LiveData<GolosUserAccountInfo>>

    fun requestUsersAccountInfoUpdate(golosUserName: List<String>,
                                      completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun getGolosUserSubscribers(golosUserName: String): LiveData<List<String>>

    fun requestGolosUserSubscribersUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun getGolosUserSubscriptions(golosUserName: String): LiveData<List<String>>

    fun requestGolosUserSubscriptionsUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    fun subscribeOnGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    fun unSubscribeFromGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    fun addAccountInfo(list: List<GolosUserAccountInfo>)

    val currentUserSubscriptionsUpdateStatus: LiveData<Map<String, UpdatingState>>
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
}

class UsersRepositoryImpl(private val mPersister: GolosUsersPersister,
                          private val mCurrentUserInfo: UserDataProvider,
                          private val mGolosApi: GolosUsersApi,
                          private val mWorkerExecutor: Executor = NamedExecutor("users repo executor"),
                          private val mMainThreadExecutor: Executor = MainThreadExecutor(),
                          private val mUserInfoRefreshDelay: Long = TimeUnit.DAYS.toMillis(5),
                          private val mLogger: ExceptionLogger = FabricExceptionLogger) : GolosUsersRepository {


    private val mGolosUsers: HashMap<String, MutableLiveData<GolosUserAccountInfo>> = HashMap()
    private val mUsersSubscriptions: HashMap<String, MutableLiveData<List<String>>> = HashMap()
    private val mUsersSubscribers: HashMap<String, MutableLiveData<List<String>>> = HashMap()
    private val mUpdatingStates = MutableLiveData<HashMap<String, UpdatingState>>()

    override fun getGolosUserAccountInfos(names: List<String>): List<LiveData<GolosUserAccountInfo>> {
        val out = arrayListOf<LiveData<GolosUserAccountInfo>>()

        names.forEach {
            if (!mGolosUsers.containsKey(it)) mGolosUsers[it] = MutableLiveData()
            out.add(mGolosUsers[it]!!)
        }
        return out
    }

    override fun requestUsersAccountInfoUpdate(golosUserName: List<String>,
                                               completionHandler: (Unit, GolosError?) -> Unit) {
        mWorkerExecutor.execute {
            try {
                val users = mGolosApi.getGolosUsers(golosUserName, golosUserName.size == 1)
                mMainThreadExecutor.execute {
                    golosUserName.forEach {
                        if (!mGolosUsers.containsKey(it)) mGolosUsers[it] = MutableLiveData()
                    }
                    mGolosUsers.forEach {
                        it.value.value = users[it.key]
                    }

                }
                mPersister.saveGolosUsersAccountInfo(users.map { it.value })
            } catch (e: Exception) {
                mLogger.log(e)
                mMainThreadExecutor.execute {
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun addAccountInfo(list: List<GolosUserAccountInfo>) {
        mMainThreadExecutor.execute {
            list.forEach { it ->
                if (!mGolosUsers.containsKey(it.userName)) mGolosUsers[it.userName] = MutableLiveData()
                mGolosUsers[it.userName]?.value = it
            }
        }
        mWorkerExecutor.execute { mPersister.saveGolosUsersAccountInfo(list) }
    }

    override fun requestGolosUserSubscribersUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit) {
        if (!mUsersSubscribers.containsKey(golosUserName)) mUsersSubscribers[golosUserName] = MutableLiveData()
        mWorkerExecutor.execute {
            try {
                val subscribers = mGolosApi.getGolosUserSubscribers(golosUserName, null)
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
        if (isUserLoggedIn) {
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


        val updatingStates = mUpdatingStates.value ?: hashMapOf()
        updatingStates[user] = UpdatingState.UPDATING
        mUpdatingStates.value = updatingStates

        mWorkerExecutor.execute {
            try {
                if (isFollow) mGolosApi.subscribe(user) else mGolosApi.unSubscribe(user)

                mMainThreadExecutor.execute {
                    requestGolosUserSubscribersUpdate(currentUserName)
                    updatingStates[user] = UpdatingState.DONE
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
                val subscriptions = mGolosApi.getGolosUserSubscriptions(golosUserName, null)
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
    }

    private fun addSubscribers(golosUsersSubscribers: Map<String, List<String>>) {
        mMainThreadExecutor.execute {
            golosUsersSubscribers.forEach {
                if (!mUsersSubscribers.contains(it.key)) mUsersSubscribers[it.key] = MutableLiveData()
                mUsersSubscribers[it.key]?.value = it.value
            }
        }
    }

    private fun addSubscriptions(golosUsersSubscriptions: Map<String, List<String>>) {
        mMainThreadExecutor.execute {
            golosUsersSubscriptions.forEach {
                if (!mUsersSubscriptions.contains(it.key)) mUsersSubscriptions[it.key] = MutableLiveData()
                mUsersSubscriptions[it.key]?.value = it.value
            }
        }
    }
}