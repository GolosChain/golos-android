package io.golos.golos.notifications

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import io.golos.golos.MockPersister
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.utils.to
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class GolosServicesApiTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val thread = Thread.currentThread()
    val executor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var mGolosServiceApi: GolosServicesInteractionManager

    @Before
    fun before() {
        val tokenProvider = object : FCMTokenProvider {
            private val _mLiveData = MutableLiveData<String>()

            init {
                _mLiveData.value = UUID.randomUUID().toString()
            }

            override val onTokenChange: LiveData<String> = _mLiveData
        }
        val userDataProvider = object : UserDataProvider {
            private val _mLiveData = MutableLiveData<AppUserData>()

            init {
                _mLiveData.value = AppUserData(true,
                        null, null,
                        "yuri-vlad-second", null,
                        null, null, null,
                        0, 0L, 0.0,
                        0.0, 0.0, 0.0, 0L, 0.0,
                        0.0, 0, null, null, 0L, null)
            }

            override val appUserData: LiveData<AppUserData> = _mLiveData
        }
        Golos4J.getInstance().addAccount(AccountName("yuri-vlad-second"),
                PrivateKeyType.POSTING to "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3",
                true)

        mGolosServiceApi = GolosServicesInteractionManager(workerExecutor = executor,
                tokenProvider = tokenProvider,
                userDataProvider = userDataProvider,
                notificationsPersister = MockPersister)
        mGolosServiceApi.setUp()
    }


    @Test
    fun test1() {
        // GolosServicesSocketHandler(BuildConfig.GATE_URL).sendMessage(GolosAuthResponse("sdgsgd", "sgsdgsgd"), "auth")

        Thread.sleep(120_000)

    }
}