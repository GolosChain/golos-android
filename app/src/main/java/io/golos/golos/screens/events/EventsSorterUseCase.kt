package io.golos.golos.screens.events

import io.golos.golos.R
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.utils.StringProvider
import timber.log.Timber
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*


class EventsSorterUseCase(private val stringProvider: StringProvider) {
    private val previousCalenar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    private val currentTimeZone = TimeZone.getDefault()
    private val currentCalenar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    private val today = Calendar.getInstance(TimeZone.getDefault())
    private val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault()).apply {
        if (Locale.getDefault() == Locale("ru", "RU")) {
            val newMonths = arrayOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
            DateFormatSymbols.getInstance(Locale.getDefault()).let { dfs ->
                {
                    dfs.months = newMonths
                    dateFormatSymbols = dfs
                }
            }
        }
    }

    // events must be sorted by date
    fun getListItems(events: List<GolosEvent>): List<EventsListItem> {
        val out = ArrayList<EventsListItem>(events.size)

        events.forEachIndexed { i, e ->
            currentCalenar.timeInMillis = events[i].creationTime  + currentTimeZone.getOffset( events[i].creationTime)
            previousCalenar.timeInMillis =
                    if (i == 0) today.timeInMillis else events[i - 1].creationTime + currentTimeZone.getOffset( events[i - 1].creationTime)
            if (previousCalenar.get(Calendar.DAY_OF_YEAR) != currentCalenar.get(Calendar.DAY_OF_YEAR)) {
                val day = currentCalenar.get(Calendar.DAY_OF_YEAR)
                val today = today.get(Calendar.DAY_OF_YEAR)
                val diff = today - day
                out.add(DateMarkContainingItem(when (diff) {
                    1 -> stringProvider.get(R.string.yestarday)
                    else -> sdf.format(events[i].creationTime)
                }))
            }
            out.add(EventContainingItem(e))
        }

        return out
    }
}

sealed class EventsListItem

class EventContainingItem(val event: GolosEvent) : EventsListItem() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventContainingItem) return false

        if (event != other.event) return false

        return true
    }

    override fun hashCode(): Int {
        return event.hashCode()
    }

    override fun toString(): String {
        return "EventContainingItem(event=$event)"
    }


}

data class DateMarkContainingItem(val date: String) : EventsListItem()