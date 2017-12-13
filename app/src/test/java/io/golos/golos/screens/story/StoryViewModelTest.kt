package io.golos.golos.screens.story

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.utils.GolosLinkMatcher
import io.golos.golos.utils.StoryLinkMatch
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor

/**
 * Created by yuri on 13.12.17.
 */
class StoryViewModelTest {
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
    private lateinit var storyViewModel: StoryViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                executor,
                executor,
                object : Persister() {
                    var users = HashMap<String, String>()
                    var userData: UserData? = null
                    var name: String? = null
                    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
                        users.put(userName, avatarPath + "__" + updatedDate)

                    }

                    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
                        if (!users.containsKey(userName)) return null
                        val path = users.get(userName)!!.split("__")
                        return Pair(path[0].replace("__", ""),
                                path[1].replace("__", "").toLong())
                    }

                    override fun getActiveUserData(): UserData? = userData

                    override fun saveUserData(userData: UserData) {
                        this.userData = userData
                    }

                    override fun deleteUserData() {
                        userData = null
                    }

                    override fun getCurrentUserName() = userData?.userName

                    override fun saveCurrentUserName(name: String?) {
                        this.name = name
                    }
                }, ApiImpl()
        )
        storyViewModel = StoryViewModel()
        storyViewModel.mRepository = repo
    }

    @Test
    fun onCreate() {
        val storyLiveData = storyViewModel.liveData
        Assert.assertNull(storyLiveData.value)
        val result = GolosLinkMatcher.match("https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/") as StoryLinkMatch

        storyViewModel.onCreate(result.author, result.permlink, result.blog, FeedType.UNCLASSIFIED)

        Assert.assertNotNull(storyLiveData.value)
    }

}