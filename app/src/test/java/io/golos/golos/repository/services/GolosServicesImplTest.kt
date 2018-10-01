package io.golos.golos.repository.services

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import io.golos.golos.MockPersister
import io.golos.golos.notifications.FCMTokenProvider
import io.golos.golos.notifications.FCMTokens
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.utils.to
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class GolosServicesImplTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val executor: Executor = Executors.newSingleThreadExecutor()
    val mainThreadExecutor: Executor = Executor { it.run() }
    private lateinit var mGolosServiceApi: GolosServices
    private val userDataProvider = MyUserDataProvider()
    @Before
    fun before() {
        val tokenProvider = object : FCMTokenProvider {
            private val _mLiveData = MutableLiveData<String>()

            init {
                _mLiveData.value = UUID.randomUUID().toString()
            }

            override val tokenLiveData: LiveData<FCMTokens> = Transformations.map(_mLiveData, {
                it ?: null
                FCMTokens(null, it!!)
            })
        }

        Golos4J.getInstance().addAccount(AccountName("yuri-vlad-second"),
                PrivateKeyType.POSTING to "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3",
                true)

        mGolosServiceApi = GolosServicesImpl(tokenProvider, userDataProvider, MockPersister,
                GolosServicesGateWayImpl(), mainThreadExecutor, mainThreadExecutor)


    }

    @Test
    fun testAuth() {

        mGolosServiceApi.setUp()

        userDataProvider.setLoggedStatus(true)
        assert(mGolosServiceApi.getEvents().value.orEmpty().isNotEmpty())
        print(mGolosServiceApi.getEvents().value)

        mGolosServiceApi.getEvents(listOf(EventType.VOTE)).value.orEmpty().let { assert(it.isNotEmpty()) }

        assert(MockPersister.isUserSubscribedOnNotificationsThroughServices())

        userDataProvider.setLoggedStatus(false)

        assert(mGolosServiceApi.getEvents().value.orEmpty().isEmpty())
        EventType.values().forEach {
            mGolosServiceApi.getEvents(listOf(it)).value.orEmpty().let { assert(it.isEmpty()) }
        }
        assert(!MockPersister.isUserSubscribedOnNotificationsThroughServices())

    }


}

class MyUserDataProvider : UserDataProvider {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()

    val _mLiveData = MutableLiveData<AppUserData>()

    fun setLoggedStatus(isLoggedIn: Boolean) {
        _mLiveData.value = AppUserData(isLoggedIn,
                null, null,
                "yuri-vlad-second", null,
                null, null, null,
                0, 0L, 0.0,
                0.0, 0.0, 0.0, 0L, 0.0,
                0.0, 0, null, null, 0L, null)
    }

    override val appUserData: LiveData<AppUserData> = Transformations.map(_mLiveData, {
        it ?: null
        it
    })
}