package io.golos.golos.repository.api


import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.util.AuthUtils
import io.golos.golos.repository.StoryFilter
import io.golos.golos.screens.stories.model.FeedType
import junit.framework.Assert
import junit.framework.Assert.*
import org.apache.commons.lang3.tuple.ImmutablePair
import org.junit.Test
import java.util.*

/**
 * Created by yuri on 23.11.17.
 */
class ApiImplTest {
    val publicActive = "GLS7feysP2A87x4uNwn68q13rF3bchD6AKhJWf4CmPWjqQF8vCb5G"
    val privateActive = "5K7YbhJZqGnw3hYzsmH5HbDixWP5ByCBdnJxM5uoe9LuMX5rcZV"
    val publicPosting = "GLS75PefYywHJtG1cDtd4JoF7NRMT7tJig69zn1YytFdbsDHatLju"
    val privatePosting = "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3"
    val accname = "yuri-vlad-second"
    val masterPassword = "234sfdgkh1ezedsiU234wewe235ym8jhlq1unA0tlkJKfdhyn"
    val keys = AuthUtils.generatePrivateWiFs(accname, masterPassword, PrivateKeyType.values())
    val publicKeys = AuthUtils.generatePublicWiFs(accname, masterPassword, PrivateKeyType.values())
    private val service = ApiImpl()
    @Test
    fun uploadImage() {
        val image = io.golos.golos.Utils.getFileFromResources("back_rect.png")
        Golos4J.getInstance().addAccount(AccountName(accname), ImmutablePair(PrivateKeyType.POSTING,
                privatePosting),
                true)
        var imageName = service.uploadImage(accname, image)
        println(imageName)
        assertNotNull(imageName)

        Golos4J.getInstance().addAccount(AccountName(accname), ImmutablePair(PrivateKeyType.ACTIVE,
                keys.get(PrivateKeyType.ACTIVE)), true);
        imageName = service.uploadImage(accname, image)
        assertNotNull(imageName)
    }

    @Test
    fun authTest() {

        assertTrue(AuthUtils.isWiFsValid(privateActive, publicActive))
        assertTrue(AuthUtils.isWiFsValid(privatePosting, publicPosting))

        var resp = service.auth(accname, masterPassword, null, null)
        assertNotNull(resp.accountInfo.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertEquals(publicPosting, resp.postingAuth!!.first)
        assertEquals(privatePosting, resp.postingAuth!!.second)
        assertEquals(publicActive, resp.activeAuth!!.first)
        assertEquals(1, resp.accountInfo.subscribersCount)
        assertEquals(3, resp.accountInfo.subscibesCount)
        assertEquals("тили тили, хали-гали, это мы не проходили, это нам не задавали,  парам пам пам.",
                resp.accountInfo.userMotto)
        assertTrue(resp.accountInfo.postsCount > 23)
        assertTrue(resp.accountInfo.accountWorth > 0.1)

        resp = service.auth(accname, null, privateActive, null)
        assertNotNull(resp.accountInfo.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertEquals(publicPosting, resp.postingAuth!!.first)
        assertNull(resp.postingAuth!!.second)
        assertEquals(publicActive, resp.activeAuth!!.first)
        assertEquals(privateActive, resp.activeAuth!!.second)
        assertEquals(1, resp.accountInfo.subscribersCount)
        assertEquals(3, resp.accountInfo.subscibesCount)
        assertEquals("тили тили, хали-гали, это мы не проходили, это нам не задавали,  парам пам пам.",
                resp.accountInfo.userMotto)
        assertTrue(resp.accountInfo.postsCount > 23)
        assertTrue(resp.accountInfo.accountWorth > 0.1)


        resp = service.auth(accname, null, null, privatePosting)

        assertNotNull(resp.accountInfo.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertEquals(publicPosting, resp.postingAuth!!.first)
        assertEquals(privatePosting, resp.postingAuth!!.second)
        assertEquals(publicActive, resp.activeAuth!!.first)
        assertEquals(1, resp.accountInfo.subscribersCount)
        assertEquals(3, resp.accountInfo.subscibesCount)
        assertEquals("тили тили, хали-гали, это мы не проходили, это нам не задавали,  парам пам пам.",
                resp.accountInfo.userMotto)
        assertNull(resp.activeAuth!!.second)
        assertTrue(resp.accountInfo.postsCount > 23)
        assertTrue(resp.accountInfo.accountWorth > 0.1)

        resp = service.auth(accname, null, "dasgsdg", null)

        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, null, "sdgsdg")
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, "sdgsdg", "sdgsdg")

        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname + "afsafs", null, privateActive, privatePosting)

        assertFalse(resp.isKeyValid)
        assertEquals(accname + "afsafs", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, null, null)
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)

        resp = service.auth(accname, "asasgasgasg", null, null)
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)



        resp = service.auth("fair", null, privateActive, privatePosting)

        assertFalse(resp.isKeyValid)
        assertEquals("fair", resp.accountInfo.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)
    }

    @Test
    fun postFirstLevelCommentTest() {
        Thread.sleep(4000)
        Golos4J.getInstance().addKeysToAccount(AccountName(accname), ImmutablePair(PrivateKeyType.POSTING, privatePosting))
        val storyTree = service.getStory("ase", "yuri-vlad-second", "123")
        assertNotNull(storyTree)
        service.sendComment("yuri-vlad-second",
                storyTree.rootStory()!!.author,
                storyTree.rootStory()!!.permlink,
                "test first level reply ${UUID.randomUUID()}",
                storyTree.rootStory()!!.categoryName)
        val updatedStoryTree = service.getStory("ase", "yuri-vlad-second", "123")
        assertEquals(storyTree.commentsWithState().size + 1, updatedStoryTree.comments().size)

    }

    @Test
    fun postSecondLevelTest() {
        Golos4J.getInstance().addKeysToAccount(AccountName(accname), ImmutablePair(PrivateKeyType.POSTING, privatePosting))
        val storyTree = service.getStory("ase", "yuri-vlad-second", "123")
        assertNotNull(storyTree)
        service.sendComment("yuri-vlad-second",
                storyTree.comments().first().author,
                storyTree.comments().first().permlink,
                "test second level reply ${UUID.randomUUID()}",
                storyTree.rootStory()!!.categoryName)
        val updatedStoryTreeWithSecondLevel = service.getStory("ase", "yuri-vlad-second", "123")
        assertEquals(storyTree.comments().first().childrenCount + 1, updatedStoryTreeWithSecondLevel.comments().first().childrenCount)
    }

    @Test
    fun getAccountDataTest() {
        service.getAccountData("yuri-vlad-second")
    }

    @Test
    fun testGetAvatar() {
        val avatar = service.getUserAvatar("masterokst", "stoit-li-smotret-legendu-o-kolovrate", "ru----kino")
        Assert.assertNull(avatar)
        service.getUserAvatar("compress", "predchuvstvie-revolyucii-mirovogo-masshtaba", "ru----apvot50--50")
    }

    @Test
    fun filteredStoriesTest() {
        val stories = service.getStories(20, FeedType.ACTUAL, 10, StoryFilter("psk"), null, null)
        Assert.assertEquals(20, stories.size)
        Assert.assertFalse(stories.any { !it.rootStory()!!.tags.contains("psk") })
    }

    @Test
    fun createPostTest() {
        service.auth(accname, null, null, privatePosting)
        val tags = arrayOf<String>("first", "second", "third")
        val result = service.sendPost(accname,
                "Test Title" + UUID.randomUUID().toString().substring(8),
                " ewtwetwetwet",
                tags)
        assertEquals(accname, result.author)
        assertEquals("first", result.blog)
    }

    @Test
    fun getStoryNoComments() {
        val story = service.getStoryWithoutComments("sinte",
                "o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh")
        Assert.assertNotNull(story)
    }

    @Test
    fun getAvatarsOf() {
        val avatars = service.getUserAvatars(listOf("yuri-vlad", "yuri-vlad-second", "jevgenika", "sinte"))
        Assert.assertNotNull(avatars)
        Assert.assertEquals(4, avatars.size)
        Assert.assertNotNull(avatars["yuri-vlad-second"])
        Assert.assertNotNull(avatars["jevgenika"])
        Assert.assertNull(avatars["yuri-vlad"])
    }
}