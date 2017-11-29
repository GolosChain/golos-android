package io.golos.golos.repository.api


import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.util.AuthUtils
import io.golos.golos.repository.model.UserAuthResponse
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
        var neededResp = UserAuthResponse(true, "yuri-vlad-second",
                Pair(publicPosting, privatePosting),
                Pair(publicActive, privateActive),
                null,
                0,
                0.0)
        assertEquals(neededResp, resp)

        neededResp = UserAuthResponse(true, "yuri-vlad-second",
                Pair(publicPosting, null),
                Pair(publicActive, privateActive),
                null,
                0,
                0.0)

        resp = service.auth(accname, null, privateActive, null)
        assertEquals(neededResp, resp)

        neededResp = UserAuthResponse(true, "yuri-vlad-second",
                Pair(publicPosting, privatePosting),
                Pair(publicActive, null),
                null,
                0,
                0.0)
        resp = service.auth(accname, null, null, privatePosting)
        assertEquals(neededResp, resp)

        neededResp = UserAuthResponse(false, "yuri-vlad-second",
                Pair(publicPosting, null),
                Pair(publicActive, "dasgsdg"),
                null,
                0,
                0.0)
        resp = service.auth(accname, null, "dasgsdg", null)
        assertEquals(neededResp, resp)

        neededResp = UserAuthResponse(false, "yuri-vlad-second",
                Pair(publicPosting, "sdgsdg"),
                Pair(publicActive, null),
                null,
                0,
                0.0)
        resp = service.auth(accname, null, null, "sdgsdg")
        assertEquals(neededResp, resp)

        neededResp = UserAuthResponse(false, "yuri-vlad-second",
                Pair(publicPosting, "sdgsdg"),
                Pair(publicActive, "sdgsdg"),
                null,
                0,
                0.0)
        resp = service.auth(accname, null, "sdgsdg", "sdgsdg")
        assertEquals(neededResp, resp)

        resp = service.auth(accname, null, privateActive, privatePosting)
        neededResp = UserAuthResponse(true, "yuri-vlad-second",
                Pair(publicPosting, privatePosting),
                Pair(publicActive, privateActive),
                null,
                0,
                0.0)
        assertEquals(neededResp, resp)
    }

    @Test
    fun postFirstLevelCommentTest() {
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
    fun getAccountDataTest(){
        service.getAccountData("yuri-vlad-second")
    }

}