package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MockPersister
import io.golos.golos.MockUserSettings
import io.golos.golos.R
import io.golos.golos.repository.api.ApiImpl
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Created by yuri on 07.12.17.
 */
class RepoAuthTest {
    val userName = "yuri-vlad-second"
    val publicActive = "GLS7feysP2A87x4uNwn68q13rF3bchD6AKhJWf4CmPWjqQF8vCb5G"
    val privateActive = "5K7YbhJZqGnw3hYzsmH5HbDixWP5ByCBdnJxM5uoe9LuMX5rcZV"
    val publicPosting = "GLS75PefYywHJtG1cDtd4JoF7NRMT7tJig69zn1YytFdbsDHatLju"
    val privatePosting = "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3"
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val executor: Executor
        get() {
            return Executor { it.run() }
        }
    private lateinit var repo: RepositoryImpl
    @Before
    fun before() {
        repo = RepositoryImpl(
                executor,
                executor ,
                executor, MockPersister, ApiImpl(),
                mLogger = null,
                mUserSettings = MockUserSettings,
                mNotificationsRepository = NotificationsRepository(executor, MockPersister)
        )
    }

    @Test
    fun testAuth() {
        val authData = repo.getCurrentUserDataAsLiveData();
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())

        repo.authWithActiveWif(userName, activeWif = "5JeKh4taphREBdq1Kzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3",
                listener = {
                    Assert.assertFalse(it.isKeyValid)
                    Assert.assertNotNull(it.error)
                    Assert.assertEquals(R.string.wrong_credentials, it.error!!.localizedMessage)
                })
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())

        repo.authWithPostingWif(userName, postingWif = "asdfghlofghre",
                listener = {
                    Assert.assertFalse(it.isKeyValid)
                    Assert.assertNotNull(it.error)
                    Assert.assertEquals(R.string.wrong_credentials, it.error!!.localizedMessage)
                })
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())

        repo.authWithMasterKey(userName, masterKey = "asdsdgsdg",
                listener = {
                    Assert.assertFalse(it.isKeyValid)
                    Assert.assertNotNull(it.error)
                    Assert.assertEquals(R.string.wrong_credentials, it.error!!.localizedMessage)
                })
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())


        repo.authWithPostingWif(userName, postingWif = privatePosting,
                listener = {
                    Assert.assertTrue(it.isKeyValid)
                    val resp = it
                    Assert.assertNotNull(resp)
                    Assert.assertNull(resp.activeAuth?.second)
                    Assert.assertNotNull(resp.postingAuth?.second)
                    Assert.assertEquals(userName, resp.accountInfo.userName)
                    Assert.assertNotNull(resp.accountInfo.avatarPath)
                    Assert.assertNotNull(resp.accountInfo.userMotto)
                    Assert.assertTrue(resp.accountInfo.postsCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscibesCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscribersCount != 0L)
                    Assert.assertTrue(resp.accountInfo.gbgAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosPower != 0.0)
                    Assert.assertTrue(resp.accountInfo.subscibesCount > resp.accountInfo.subscribersCount)
                })

        Assert.assertNotNull(authData.value)
        var resp = authData.value!!
        Assert.assertNotNull(resp)
        Assert.assertNull(resp.privateActiveWif)
        Assert.assertNotNull(resp.privatePostingWif)
        Assert.assertEquals(userName, resp.userName)
        Assert.assertNotNull(resp.avatarPath)
        Assert.assertNotNull(resp.getmMoto())
        Assert.assertTrue(resp.postsCount != 0L)
        Assert.assertTrue(resp.subscibesCount != 0L)
        Assert.assertTrue(resp.subscribersCount != 0L)
        Assert.assertTrue(resp.gbgAmount != 0.0)
        Assert.assertTrue(resp.golosAmount != 0.0)
        Assert.assertTrue(resp.golosPower != 0.0)
        Assert.assertTrue(resp.subscibesCount > resp.subscribersCount)


        repo.deleteUserdata()
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())

        repo.authWithActiveWif(userName, activeWif = privateActive,
                listener = {
                    Assert.assertTrue(it.isKeyValid)
                    val resp = it
                    Assert.assertNotNull(resp)
                    Assert.assertNotNull(resp.activeAuth?.second)
                    Assert.assertNull(resp.postingAuth?.second)
                    Assert.assertEquals(userName, resp.accountInfo.userName)
                    Assert.assertNotNull(resp.accountInfo.avatarPath)
                    Assert.assertNotNull(resp.accountInfo.userMotto)
                    Assert.assertTrue(resp.accountInfo.postsCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscibesCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscribersCount != 0L)
                    Assert.assertTrue(resp.accountInfo.gbgAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosPower != 0.0)
                    Assert.assertTrue(resp.accountInfo.subscibesCount > resp.accountInfo.subscribersCount)
                })

        Assert.assertNotNull(authData.value)
        resp = authData.value!!
        Assert.assertNotNull(resp)
        Assert.assertNotNull(resp.privateActiveWif)
        Assert.assertNull(resp.privatePostingWif)
        Assert.assertEquals(userName, resp.userName)
        Assert.assertNotNull(resp.avatarPath)
        Assert.assertNotNull(resp.getmMoto())
        Assert.assertTrue(resp.postsCount != 0L)
        Assert.assertTrue(resp.subscibesCount != 0L)
        Assert.assertTrue(resp.subscribersCount != 0L)
        Assert.assertTrue(resp.gbgAmount != 0.0)
        Assert.assertTrue(resp.golosAmount != 0.0)
        Assert.assertTrue(resp.golosPower != 0.0)
        Assert.assertTrue(resp.subscibesCount > resp.subscribersCount)

        repo.deleteUserdata()
        Assert.assertNull(authData.value)
        Assert.assertFalse(repo.isUserLoggedIn())

        repo.authWithMasterKey(userName, masterKey = "234sfdgkh1ezedsiU234wewe235ym8jhlq1unA0tlkJKfdhyn",
                listener = {
                    Assert.assertTrue(it.isKeyValid)
                    val resp = it
                    Assert.assertNotNull(resp)
                    Assert.assertNotNull(resp.activeAuth?.second)
                    Assert.assertNotNull(resp.postingAuth?.second)
                    Assert.assertEquals(userName, resp.accountInfo.userName)
                    Assert.assertNotNull(resp.accountInfo.avatarPath)
                    Assert.assertNotNull(resp.accountInfo.userMotto)
                    Assert.assertTrue(resp.accountInfo.postsCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscibesCount != 0L)
                    Assert.assertTrue(resp.accountInfo.subscribersCount != 0L)
                    Assert.assertTrue(resp.accountInfo.gbgAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosAmount != 0.0)
                    Assert.assertTrue(resp.accountInfo.golosPower != 0.0)
                    Assert.assertTrue(resp.accountInfo.subscibesCount > resp.accountInfo.subscribersCount)
                })

        Assert.assertNotNull(authData.value)
        resp = authData.value!!
        Assert.assertNotNull(resp)
        Assert.assertNotNull(resp.privateActiveWif)
        Assert.assertNotNull(resp.privatePostingWif)
        Assert.assertEquals(userName, resp.userName)
        Assert.assertNotNull(resp.avatarPath)
        Assert.assertNotNull(resp.getmMoto())
        Assert.assertTrue(resp.postsCount != 0L)
        Assert.assertTrue(resp.subscibesCount != 0L)
        Assert.assertTrue(resp.subscribersCount != 0L)
        Assert.assertTrue(resp.gbgAmount != 0.0)
        Assert.assertTrue(resp.golosAmount != 0.0)
        Assert.assertTrue(resp.golosPower != 0.0)
        Assert.assertTrue(resp.subscibesCount > resp.subscribersCount)
    }

}