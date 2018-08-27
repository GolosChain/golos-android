package io.golos.golos.screens.events

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.support.v4.app.FragmentActivity
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.EventsProvider
import io.golos.golos.repository.services.EventType
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.repository.services.Sourcable
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import timber.log.Timber

class EventsViewModel : ViewModel(), Observer<List<GolosEvent>> {

    override fun onChanged(t: List<GolosEvent>?) {
        t?.let {
            mEventsList.value = EventsList(it)
        }
    }

    private val mEventsList = MutableLiveData<EventsList>()
    val eventsList = mEventsList as LiveData<EventsList>
    private var mEventsProvider: EventsProvider? = null
    private var mEventTypes: List<EventType>? = null

    fun onCreate(eventTypes: List<EventType>?,
                 eventsProvider: EventsProvider) {
        mEventsProvider = eventsProvider
        mEventTypes = eventTypes

        onChanged(mEventsProvider?.getEvents(mEventTypes)?.value)
    }


    fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            mEventsProvider?.requestEventsUpdate(mEventTypes)
        }
    }

    fun onScrollToTheEnd() {
        mEventsProvider?.requestEventsUpdate(mEventTypes,
                mEventsList.value?.events?.lastOrNull()?.id)
    }

    fun onStop() {

        mEventsProvider?.getEvents(mEventTypes)?.removeObserver(this)
    }

    fun onEventClick(fragmentActivity: FragmentActivity, it: GolosEvent) {
        if (it is PostLinkable) {
            (it as? PostLinkable)?.getLink()?.let {
                Timber.e("match it = $it")
                StoryActivity.start(fragmentActivity, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
            }
        } else if (it is Sourcable) {
            UserProfileActivity.start(fragmentActivity, it.getAuthors().firstOrNull() ?: return)
        }
    }

    fun requestUpdate() {
        onChangeVisibilityToUser(true)
    }

    fun onStart() {
        mEventsProvider?.getEvents(mEventTypes)?.observeForever(this)
    }
}