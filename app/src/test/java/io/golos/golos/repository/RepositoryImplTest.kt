package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.Utils
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.screens.editor.EditorImagePart
import io.golos.golos.screens.editor.EditorTextPart
import io.golos.golos.screens.main_stripes.model.FeedType
import junit.framework.Assert.*
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor

/**
 * Created by yuri on 23.11.17.
 */
class RepositoryImplTest {
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
                executor,
                object : Persister() {
                    var users = HashMap<String, String>()
                    var keys = HashMap<PrivateKeyType, String?>()
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

                    override fun saveKeys(keys: Map<PrivateKeyType, String?>) {
                        this.keys = HashMap(keys)
                    }

                    override fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
                        val out = HashMap<PrivateKeyType, String?>()
                        keys.forEach { t, u ->
                            if (types.contains(t)) out[t] = u
                        }
                        return out
                    }

                    override fun getCurrentUserName() = name

                    override fun saveCurrentUserName(name: String?) {
                        this.name = name
                    }
                }, ApiImpl()
        )
    }

    @Test
    fun createPost() {
        repo.authWithActiveWif("yuri-vlad-second", privateActive)
        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        repo.createPost(UUID.randomUUID().toString(), listOf(EditorTextPart("sdg", "test content", pointerPosition = null),
                image),
                listOf("ase", "гаврик"), { a, b -> print(b) })

    }

    @Test
    fun testAuth() {
        val authData = repo.getCurrentUserDataAsLiveData()
        Assert.assertNull(authData.value)
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        assertEquals(userName, authData.value!!.userName)
        assertEquals(privateActive, authData.value!!.privateActiveWif)
        assertEquals(privatePosting, authData.value!!.privatePostingWif)
        Assert.assertNull(authData.value!!.avatarPath)
    }

    @Test
    fun testAvatarUpdate() {
        val popular = repo.getStories(FeedType.POPULAR)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null)
        assertNotNull(popular.value)
        assertTrue(popular.value!!.items.any { it.rootStory()!!.avatarPath != null })
    }

    @Test
    fun createCommentTest() {
        val authData = repo.getCurrentUserDataAsLiveData()
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.NEW)
        repo.requestStoriesListUpdate(20, FeedType.NEW, null, null)
        assertNotNull(newItems.value)
        assertEquals(20, newItems.value!!.items.size)
        var notCommentedItem = newItems.value!!.items.first()
        repo.requestStoryUpdate(notCommentedItem)
        notCommentedItem = newItems.value!!.items.first()

        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        val text = EditorTextPart("sdg", "test content ${UUID.randomUUID()}", pointerPosition = null)
        repo.createComment(notCommentedItem, notCommentedItem.rootStory()!!, listOf(image, text), { _, _ -> })

        assertEquals(notCommentedItem.comments().size + 1, newItems.value!!.items.first().comments().size)
    }

    @Test
    fun createSecondLevelComment() {
        val authData = repo.getCurrentUserDataAsLiveData()
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.POPULAR)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null)
        assertNotNull(newItems.value)
        assertEquals(20, newItems.value!!.items.size)
        var notCommentedItem = newItems.value!!.items.first()
        repo.requestStoryUpdate(notCommentedItem)

        notCommentedItem = newItems.value!!.items.first()
        var lastComment = newItems.value!!.items.first().comments().last()

        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        val text = EditorTextPart("sdg", "test content ${UUID.randomUUID()}", pointerPosition = null)
        repo.createComment(notCommentedItem, lastComment, listOf(image, text), { _, _ -> })

        assertEquals(notCommentedItem.rootStory()!!.commentsCount + 1, newItems.value!!.items.first().rootStory()!!.commentsCount)
        assertEquals(lastComment.commentsCount + 1, newItems.value!!.items.first().comments().last().commentsCount)

    }

    @Test
    fun testAuthWithPosting() {
        val resp = repo.authWithPostingWif(userName, privatePosting)
        assertNotNull(resp)
        assertNotNull(resp.postingAuth?.second)
        assertNull(resp.activeAuth?.second)
        assertEquals(userName, resp.userName)
        assertNull(resp.avatarPath)
    }
}