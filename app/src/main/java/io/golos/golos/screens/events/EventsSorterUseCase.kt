package io.golos.golos.screens.events

import io.golos.golos.R
import io.golos.golos.utils.StringProvider
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
    fun getListItems(events: List<EventListItem>): List<EventsListItemWrapper> {
        val out = ArrayList<EventsListItemWrapper>(events.size)

        events.forEachIndexed { i, e ->
            currentCalenar.timeInMillis = events[i].golosEvent.creationTime + currentTimeZone.getOffset(events[i].golosEvent.creationTime)
            previousCalenar.timeInMillis =
                    if (i == 0) today.timeInMillis else events[i - 1].golosEvent.creationTime + currentTimeZone.getOffset(events[i - 1].golosEvent.creationTime)
            if (previousCalenar.get(Calendar.DAY_OF_YEAR) != currentCalenar.get(Calendar.DAY_OF_YEAR)) {
                val day = currentCalenar.get(Calendar.DAY_OF_YEAR)
                val today = today.get(Calendar.DAY_OF_YEAR)
                val diff = today - day
                out.add(DateMarkContainingItem(when (diff) {
                    1 -> stringProvider.get(R.string.yestarday)
                    else -> sdf.format(events[i].golosEvent.creationTime)
                }))
            }
            out.add(EventContainingItem(e))
        }

        return out
    }
}

sealed class EventsListItemWrapper

data class EventContainingItem(val event: EventListItem) : EventsListItemWrapper()

data class DateMarkContainingItem(val date: String) : EventsListItemWrapper()