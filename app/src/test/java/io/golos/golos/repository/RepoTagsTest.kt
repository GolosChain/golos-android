package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.MockUserSettings
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.model.Tag
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Created by yuri on 08.01.18.
 */
class RepoTagsTest {
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
                MainThreadExecutor,
                MainThreadExecutor ,
                MainThreadExecutor,
                MockPersister,  mLogger = null,
                mUserSettings = MockUserSettings,
                mNotificationsRepository = NotificationsRepository(executor, MockPersister)
        )
    }

    @Test
    fun testTags() {
        var tags: List<Tag>? = null
        repo.getTrendingTags().observeForever {
            tags = it
        }
        Assert.assertTrue(tags == null)

        repo.requestTrendingTagsUpdate({ _, _ -> })
        Assert.assertTrue(tags != null)
        Assert.assertTrue(tags!!.size > 2500)
    }
}