package io.golos.golos.screens.events

import android.arch.lifecycle.*
import android.support.v4.app.FragmentActivity
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.EventsProvider
import io.golos.golos.repository.GolosUsersRepository
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
    private var mUsersProvider: GolosUsersRepository? = null
    private val updateLimit = 15


    fun onCreate(eventTypes: List<EventType>?,
                 eventsProvider: EventsProvider,
                 eventsSorter: EventsSorterUseCase?,
                 usersProvider: GolosUsersRepository?) {
        mEventsProvider = eventsProvider
        mEventTypes = eventTypes
        mEventsSorter = eventsSorter
        mUsersProvider = usersProvider

        onLiveDataChanged()
    }

    private fun onLiveDataChanged() {
        val events = mEventsProvider?.getEvents(mEventTypes)?.value.orEmpty()

        val users = mUsersProvider?.getGolosUserAccountInfos()?.value.orEmpty()
        events.filter { it is Authorable }.forEach {
            it.avatarPath = users[(it as Authorable).getAuthors().firstOrNull()]?.avatarPath
        }
        mEventsList.value = EventsList(events)
        if (events.isNotEmpty()) {
            val authorsWithNoAvatars = mEventsList.value?.events
                    .orEmpty()
                    .filter { it.avatarPath == null && it is Authorable }
                    .map { (it as Authorable).getAuthors().firstOrNull() }
                    .distinct()
                    .filter {
                        !mUsersProvider?.getGolosUserAccountInfos()?.value.orEmpty().containsKey(it)
                    }
                    .filterNotNull()

            if (authorsWithNoAvatars.isNotEmpty()) mUsersProvider?.requestUsersAccountInfoUpdate(authorsWithNoAvatars)
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
        mUsersProvider?.let { mEventsList.removeSource(it.getGolosUserAccountInfos()) }
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
        mUsersProvider?.let {
            mEventsList.addSource(it.getGolosUserAccountInfos()) {
                onLiveDataChanged()
            }
        }
    }
}