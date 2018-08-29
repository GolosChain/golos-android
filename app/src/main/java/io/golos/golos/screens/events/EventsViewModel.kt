package io.golos.golos.screens.events

import android.arch.lifecycle.*
import android.support.v4.app.FragmentActivity
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.AvatarRepository
import io.golos.golos.repository.EventsProvider
import io.golos.golos.repository.services.Authorable
import io.golos.golos.repository.services.EventType
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import timber.log.Timber

class EventsViewModel : ViewModel() {

    private val mEventsList = MediatorLiveData<EventsList>()
    val eventsList: LiveData<List<EventsListItem>> = Transformations.map(mEventsList) {
        getSorter()?.getListItems(it.events)
    }
    val updateState = MutableLiveData<Boolean>() as LiveData<Boolean>
    private var mEventsProvider: EventsProvider? = null
    private var mEventTypes: List<EventType>? = null
    private var mEventsSorter: EventsSorterUseCase? = null
    private var mAvatarsProvider: AvatarRepository? = null
    private var lastEventsSize: Int = 0
    private val updateLimit = 15


    fun onCreate(eventTypes: List<EventType>?,
                 eventsProvider: EventsProvider,
                 eventsSorter: EventsSorterUseCase?,
                 avatarsProvider: AvatarRepository?) {
        mEventsProvider = eventsProvider
        mEventTypes = eventTypes
        mEventsSorter = eventsSorter
        mAvatarsProvider = avatarsProvider

        onLiveDataChanged()
    }

    private fun onLiveDataChanged() {
        val events = mEventsProvider?.getEvents(mEventTypes)?.value.orEmpty()
        val avatars = mAvatarsProvider?.avatars?.value.orEmpty()
        events.filter { it is Authorable }.forEach {
            it.avatarPath = avatars[(it as Authorable).getAuthors().firstOrNull()]
        }
        mEventsList.value = EventsList(events)
        if (lastEventsSize != events.size) {
            lastEventsSize = events.size
            mAvatarsProvider?.requestAvatarsUpdate(events
                    .filter {
                        it is Authorable
                                && it.avatarPath == null
                                && it.getAuthors().isNotEmpty()
                    }
                    .map { (it as Authorable).getAuthors().first() })
        }
    }


    fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            mEventsProvider?.requestEventsUpdate(mEventTypes, limit = updateLimit)
        }
    }

    private fun getSorter() = mEventsSorter

    fun onScrollToTheEnd() {
        mEventsProvider?.requestEventsUpdate(mEventTypes,
                mEventsList.value?.events?.lastOrNull()?.id, updateLimit) { _, _ -> (updateState as MutableLiveData<Boolean>).value = false }
        (updateState as MutableLiveData<Boolean>).value = true
    }

    fun onStop() {
        mEventsProvider?.let { mEventsList.removeSource(it.getEvents(mEventTypes)) }
        mAvatarsProvider?.let { mEventsList.removeSource(it.avatars) }
    }

    fun onEventClick(fragmentActivity: FragmentActivity, it: GolosEvent) {
        if (it is PostLinkable) {
            (it as? PostLinkable)?.getLink()?.let {
                Timber.e("match it = $it")
                StoryActivity.start(fragmentActivity, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
            }
        } else if (it is Authorable) {
            UserProfileActivity.start(fragmentActivity, it.getAuthors().firstOrNull() ?: return)
        }
    }

    fun requestUpdate() {
        onChangeVisibilityToUser(true)
    }

    fun onStart() {
        mEventsProvider?.let {
            mEventsList.addSource(it.getEvents(mEventTypes)) {
                onLiveDataChanged()
            }
        }
        mAvatarsProvider?.let {
            mEventsList.addSource(it.avatars) {
                onLiveDataChanged()
            }
        }
    }
}