package io.golos.golos.repository.api


import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.base.models.DiscussionQuery
import eu.bittrade.libs.golosj.base.models.Permlink
import eu.bittrade.libs.golosj.base.models.operations.AccountUpdateOperation
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import eu.bittrade.libs.golosj.util.AuthUtils
import eu.bittrade.libs.golosj.util.ImmutablePair
import io.golos.golos.Utils
import io.golos.golos.repository.model.DiscussionItemFactory
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.utils.RSharesConverter
import junit.framework.Assert
import junit.framework.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.util.*
import kotlin.collections.HashSet

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
        assertNotNull(resp.accountInfo.golosUser.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
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
        assertNotNull(resp.accountInfo.golosUser.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
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

        assertNotNull(resp.accountInfo.golosUser.avatarPath)
        assertTrue(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
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
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, null, "sdgsdg")
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, "sdgsdg", "sdgsdg")

        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname + "afsafs", null, privateActive, privatePosting)

        assertFalse(resp.isKeyValid)
        assertEquals(accname + "afsafs", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)


        resp = service.auth(accname, null, null, null)
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)

        resp = service.auth(accname, "asasgasgasg", null, null)
        assertFalse(resp.isKeyValid)
        assertEquals("yuri-vlad-second", resp.accountInfo.golosUser.userName)
        assertNull(resp.postingAuth)
        assertNull(resp.activeAuth)



        resp = service.auth("fair", null, privateActive, privatePosting)

        assertFalse(resp.isKeyValid)
        assertEquals("fair", resp.accountInfo.golosUser.userName)
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
        service.getAccountInfo("yuri-vlad-second")
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
        val tags = arrayOf<String>("test", "second", "third")
        val result = service.sendPost(accname,
                "Test Title" + UUID.randomUUID().toString().substring(8),
                " ewtwetwetwet",
                tags)
        assertEquals(accname, result.author)
        assertEquals("test", result.blog)


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

    @Test
    fun getFollowersTest() {
        val fObject = Golos4J.getInstance().followApiMethods.getFollowCount(AccountName("vredinka2345"))
        var frs = service.getSubscriptions("yuri-vlad", null)
        Assert.assertEquals(6, frs.size)
        frs = service.getSubscriptions("vredinka2345", null)
        Assert.assertEquals(fObject.followingCount, frs.size)


        frs = service.getSubscriptions("vredinka2345", "med")
        Assert.assertEquals(fObject.followingCount - 1, frs.size)

        frs = service.getSubscribers("vredinka2345", null)
        Assert.assertEquals(fObject.followerCount, frs.size)
    }

    @Test
    fun getFollowersTest2() {
        var frs = service.getSubscriptions("vox-populi", null)
        println(frs)
    }

    @Test
    fun followAndUnfolloweTest() {
        service.auth(accname, null, null, privatePosting)
        Golos4J.getInstance().setDefaultAccount(AccountName(accname))
        Golos4J.getInstance().simplifiedOperations.follow(AccountName("vredinka2345"))
        var followers = service.getSubscriptions(accname, null)
        Assert.assertTrue(followers.find { it.following.name == "vredinka2345" } != null)
        Golos4J.getInstance().simplifiedOperations.unfollow(AccountName("vredinka2345"))

        followers = service.getSubscriptions(accname, null)

        Assert.assertTrue(followers.find { it.following.name == "vredinka2345" } == null)

    }

    @Test
    fun testGetTags() {
        var size = 1000
        var tags = service.getTrendingTag("", size)
        Assert.assertEquals(size, tags.size)
        var tagsSet = HashSet(tags)
        Assert.assertEquals(size, tagsSet.size)

        size = 1500
        tags = service.getTrendingTag("", size)
        Assert.assertEquals(size, tags.size)
        tagsSet = HashSet(tags)
        Assert.assertEquals(size, tagsSet.size)

        size = 3200
        tags = service.getTrendingTag("", size)
        Assert.assertEquals(size, tags.size)
        tagsSet = HashSet(tags)
        Assert.assertEquals(size, tagsSet.size)
    }

    @Test
    fun testUpdateAccount() {
        val acc = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("lokkie")))
        val accUpdateOperation = AccountUpdateOperation(AccountName("yuri-vlad-second"), null, null, null, null, "")

    }

    private fun calculate_vshares(rshares: BigInteger): BigInteger {
        val s = BigInteger("2000000000000")
        // (rshares + s) * (rshares + s) - s * s
        /* var sum = rshares.add(s)
         sum = sum.multiply(sum)
         val ccs = s.multiply(s)
         sum = sum.subtract(ccs)*/
        return (rshares + s).pow(2) - s.pow(2)
    }

    private fun getMedianPrice(): Double {
        /*
            feed_history = get_feed_history()
            current_median_history = feed_history.current_median_history

            baseAmount = current_median_history.base.split(" ")[0]
            quoteAmount = current_median_history.quote.split(" ")[0]
        */
        val baseAmount = 1.000
        val quoteAmount = 0.115
        return baseAmount / quoteAmount
    }

    @Test
    fun test1() {
        // props = get_dynamic_global_properties()
        // props.total_reward_shares2
        val total_reward_shares2 = "5133376796721014669412091165625"
        // content.active_votes -> voter.rshares
        val vote_rshares = "122177915528"

        val total_reward_fund_steem = 34482.399 // 34482.399 GOLOS
        val medianPrice = getMedianPrice()

        // props.total_reward_fund_steem
        var pot = total_reward_fund_steem
        pot *= medianPrice
        pot = java.lang.Double.parseDouble(String.format("%.3f", pot))

        val total_r2 = BigInteger(total_reward_shares2)

        var r2 = calculate_vshares(BigInteger(vote_rshares))
        r2 = r2.multiply(BigInteger(pot.toString().replace(".", "")))
        r2 = r2.divide(total_r2)

        val result = r2.toString().toLong()
        print((result / 1000).toString() + "." + result % 1000)
        println(" GBG")
    }

    @Test
    fun testConvertRsharesToGbg() {
        val dq = DiscussionQuery()
        dq.limit = 2
        dq.truncateBody = 1
        //   val postRaw = Golos4J.getInstance().databaseMethods.getDiscussionsLightBy(dq, DiscussionSortType.GET_DISCUSSIONS_BY_HOT)[0]
        val post = Utils.readStoriesFromResourse("stripe.json")[0].rootStory()!!

        var votesRshares = BigInteger.ZERO
        // val properties = Golos4J.getInstance().databaseMethods.dynamicGlobalProperties

        val current = System.currentTimeMillis()
        var out: List<Double> = arrayListOf()

        (0 until 1).forEach {
            val votes = post.activeVotes.map { it.rshares }

            out = RSharesConverter.convertRSharesToGbg2(post.gbgAmount, votes,
                    post.votesRshares)

            println("payout must be ${post.gbgAmount}")
            var calculated = 0.0
            out.forEach { calculated += it }
            println("my calculated is ${calculated} they differs in ${(1 - (post.gbgAmount / calculated)) * 100}%")

        }

        println("time elapsed is ${System.currentTimeMillis() - current}")//3971
    }

    @Test
    fun getStory() {
        val story = Golos4J
                .getInstance().databaseMethods
                .getContent(AccountName("vp-golos-est"), Permlink("golos-est-ragu-s-gribami-postnoe-blyudo"))
        val rows = StoryParserToRows.parse(DiscussionItemFactory.create(story!!, null))
        println(story)
    }

    @Test
    fun getStoryWithManycommentsText() {
        val story = Golos4J.getInstance().databaseMethods.getStoryWithRepliesAndInvolvedAccounts(AccountName("vp-zarubezhje"),
                Permlink("lichnyi-opyt-kak-ya-vengerskii-yazyk-uchila"), -1)
    }

    @Test
    fun getAccountAdditionalDataTest() {
        val account = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("yuri-vlad-second")))
        println(account)
    }

}