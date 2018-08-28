package io.golos.golos.screens.events

import io.golos.golos.R
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.utils.StringProvider
import java.util.*

class EventsSorterUseCase(private val stringProvider: StringProvider) {
    private val previousCalenar = Calendar.getInstance(TimeZone.getDefault())
    private val currentCalenar = Calendar.getInstance(TimeZone.getDefault())
    private val today = Calendar.getInstance(TimeZone.getDefault())

    // events must be sorted by date
    fun getListItems(events: List<GolosEvent>): List<EventsListItem> {
        val out = ArrayList<EventsListItem>(events.size)

        events.forEachIndexed { i, e ->
            currentCalenar.timeInMillis = events[i].creationTime


            previousCalenar.timeInMillis =
                    if (i == 0) today.timeInMillis else events[i - 1].creationTime

            if (previousCalenar.get(Calendar.DAY_OF_YEAR) != currentCalenar.get(Calendar.DAY_OF_YEAR)) {
                val day = currentCalenar.get(Calendar.DAY_OF_YEAR)
                val today = today.get(Calendar.DAY_OF_YEAR)
                val diff = today - day
                out.add(DateMarkContainingItem(when (diff) {
                    1 -> stringProvider.get(R.string.yestarday)
                    else -> String.format("%02d", currentCalenar.get(Calendar.DAY_OF_MONTH)) +
                            ".${String.format("%02d", currentCalenar.get(Calendar.MONTH))}" +
                            ".${currentCalenar.get(Calendar.YEAR)}"
                }))
            }
            out.add(EventContainingItem(e))
        }

        return out
    }
}

sealed class EventsListItem

data class EventContainingItem(val event: GolosEvent) : EventsListItem()
data class DateMarkContainingItem(val date: String) : EventsListItem()