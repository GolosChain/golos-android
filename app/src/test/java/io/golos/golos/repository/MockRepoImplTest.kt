package io.golos.golos.repository

import ParseTest
import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

public class MockRepoImplTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()

    val executor: Executor
        get() {
            return Executor { it.run() }
        }

    @Test
    fun voteTest() {
        var popularItems: List<StoryTree>? = null
        val golosApi = mock<GolosApi> {
            on {
                getStories(20, FeedType.POPULAR, 1024, null, null)
            } doReturn ArrayList(ParseTest.readStoriesFromResourse("stripes_6.json")) as List<StoryTree>
            on {
                getStory("ru--apvot50-50", "lokkie", "golos-gajd-finalnyj-post-obshchie-sovety-dlya-vseh-i-kazhdogo-5050-11-19-2")
            } doReturn ParseTest.readStoryFromResourse("story_6.json")

        }
        val mockRepoImpl = MockRepoImpl(golosApi, executor, executor)
        mockRepoImpl.getStories(FeedType.POPULAR).observeForever {
            popularItems = it?.items
        }
        mockRepoImpl.requestStoriesListUpdate(20, FeedType.POPULAR, null, null)
        assertEquals(20, popularItems!!.size)

        val firstItem = popularItems!![0].deepCopy()
        Assert.assertTrue(popularItems!![0].rootStory()!!.isUserUpvotedOnThis == false)
        mockRepoImpl.upVote(popularItems!![0].rootStory()!!, 100.toShort())

        assertEquals(20, popularItems!!.size)
        Assert.assertTrue(popularItems!![0].rootStory()!!.gbgAmount > firstItem.rootStory()!!.gbgAmount)
        Assert.assertTrue(popularItems!![0].rootStory()!!.isUserUpvotedOnThis == true)

        mockRepoImpl.requestStoryUpdate(firstItem)
        val updatedFirstitem = popularItems!![0].deepCopy()
        assertEquals(19, updatedFirstitem.getFlataned().size)

        val firstComment = popularItems!!.first().comments().first().copy()

        mockRepoImpl.upVote(firstComment, 100.toShort())

        Assert.assertTrue(popularItems!!.first().comments().first().gbgAmount > firstComment.gbgAmount)
        Assert.assertTrue(popularItems!!.first().comments().first().isUserUpvotedOnThis == true)

        val secondLevelComment = popularItems!!.first().comments().first().children.first().copy().story
        mockRepoImpl.upVote(secondLevelComment, 100.toShort())

        Assert.assertTrue(popularItems!!.first().comments().first().children.first().story.gbgAmount > secondLevelComment.gbgAmount)
        Assert.assertTrue(popularItems!!.first().comments().first().children.first().story.isUserUpvotedOnThis == true)

    }

}
