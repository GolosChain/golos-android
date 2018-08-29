package io.golos.golos.screens.events

import com.fasterxml.jackson.module.kotlin.readValue
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.repository.services.GolosServicesResponse
import io.golos.golos.repository.services.getEventData
import io.golos.golos.utils.StringProvider
import io.golos.golos.utils.mapper
import junit.framework.Assert
import org.junit.Test
import java.io.File

class EventsSorterUseCaseTest {
    val useCase = EventsSorterUseCase(object : StringProvider {
        override fun get(resId: Int, args: String?): String {
            return resId.toString()
        }
    })

    @Test
    fun getListItems() {
        val events = getStories("events.json")
        val listItems = useCase.getListItems(events)
        Assert.assertTrue(listItems.first() is DateMarkContainingItem)

    }

    fun getStories(path: String): List<GolosEvent> {

        val f = File(this::class.java.classLoader.getResource(path).toURI())
        val resp = mapper.readValue<GolosServicesResponse>(f)

        return resp.getEventData().map { GolosEvent.fromEvent(it) }
    }
}