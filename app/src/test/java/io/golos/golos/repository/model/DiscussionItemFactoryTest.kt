package io.golos.golos.repository.model

import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.base.models.DiscussionQuery
import eu.bittrade.libs.golosj.enums.DiscussionSortType
import junit.framework.Assert
import org.junit.Test

/**
 * Created by yuri on 27.11.17.
 */
class DiscussionItemFactoryTest {
    @Test
    fun create() {
        var query = DiscussionQuery()
        query.truncateBody = 1024
        query.limit = 2
        val discussions = Golos4J.getInstance().databaseMethods.getDiscussionsBy(query, DiscussionSortType.GET_DISCUSSIONS_BY_HOT)
        val lightDiscussion = Golos4J.getInstance().databaseMethods.getDiscussionsLightBy(query, DiscussionSortType.GET_DISCUSSIONS_BY_HOT)
        val traditional = discussions.map { GolosDiscussionItem(it, null) }
        val factoryTraditional = discussions.map { DiscussionItemFactory.create(it, null) }
        val factoryLightn = lightDiscussion.map { DiscussionItemFactory.create(it, null) }

        Assert.assertEquals(traditional, factoryTraditional)
        Assert.assertEquals(traditional, factoryLightn)
    }
}