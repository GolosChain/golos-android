package io.golos.golos.screens.events

import android.arch.lifecycle.*
import android.support.v4.app.FragmentActivity
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.EventsProvider
import io.golos.golos.repository.GolosUsersRepository
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.services.*
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.utils.UpdatingState
import timber.log.Timber

data class EventsList(val events: List<EventListItem>)

class EventsViewModel : ViewModel() {

    private val mEventsList = MediatorLiveData<EventsList>()
    val eventsList: LiveData<List<EventsListItemWrapper>> = Transformations.map(mEventsList) {
        getSorter()?.getListItems(it.events)
    }
    val updateState = MutableLiveData<Boolean>() as LiveData<Boolean>
    private var mEventsProvider: EventsProvider? = null
    private var mEventTypes: Set<EventType>? = null
    private var mEventsSorter: EventsSorterUseCase? = null
    private var mUsersProvider: GolosUsersRepository? = null
    private var mUserDataProvider: UserDataProvider? = null
    private val updateLimit = 15

    fun onCreate(eventTypes: List<EventType>?,
                 eventsProvider: EventsProvider,
                 eventsSorter: EventsSorterUseCase?,
                 usersProvider: GolosUsersRepository?,
                 userStatusProvider: UserDataProvider?) {

        mEventsProvider = eventsProvider
        if (eventTypes != null) {
            val types = HashSet<EventType>()
            types.addAll(eventTypes)
            mEventTypes = types
        }

        mEventsSorter = eventsSorter
        mUsersProvider = usersProvider
        mUserDataProvider = userStatusProvider
    }

    private fun onLiveDataChanged() {
        val events = mEventsProvider?.getEvents(mEventTypes?.toList())?.value.orEmpty()

        val avatars = mUsersProvider?.usersAvatars?.value.orEmpty()

        val out = ArrayList<EventListItem>(events.size)

        val currentUserSubscriptions = mUsersProvider?.currentUserSubscriptions?.value.orEmpty()
        val currentSubscriptionsProgress = mUsersProvider?.currentUserSubscriptionsUpdateStatus?.value.orEmpty()

        events.forEach {
            val item: EventListItem = when (it) {
                is GolosVoteEvent -> VoteEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosFlagEvent -> FlagEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosTransferEvent -> TransferEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosSubscribeEvent -> {
                    val subscribeStatus: SubscribeStatus =
                            SubscribeStatus.create(currentUserSubscriptions.contains(avatars[it.fromUsers.firstOrNull().orEmpty()])
                                    , currentSubscriptionsProgress.get(it.fromUsers.firstOrNull().orEmpty())
                                    ?: UpdatingState.DONE)

                    SubscribeEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()],
                            subscribeStatus, !currentUserSubscriptions.contains(it.fromUsers.firstOrNull().orEmpty()))
                }
                is GolosUnSubscribeEvent -> UnSubscribeEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosReplyEvent -> ReplyEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosRepostEvent -> RepostEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosMentionEvent -> MentionEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosAwardEvent -> AwardEventListItem(it, 0.0f)
                is GolosCuratorAwardEvent -> CuratorAwardEventListItem(it, 0.0f)
                is GolosMessageEvent -> MessageEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosWitnessVoteEvent -> WitnessVoteEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                is GolosWitnessCancelVoteEvent -> WitnessCancelVoteEventListItem(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
            }
            out.add(item)
        }

        mEventsList.value = EventsList(out)

        if (events.isNotEmpty()) {
            val authorsWithNoAvatars = out
                    .filter { it.avatarPath == null && it.golosEvent is Authorable }
                    .map { (it.golosEvent as Authorable).getAuthors().firstOrNull() }
                    .distinct()
                    .filter {
                        !mUsersProvider?.usersAvatars?.value.orEmpty().containsKey(it)
                    }
                    .filterNotNull()

            if (authorsWithNoAvatars.isNotEmpty()) mUsersProvider?.requestUsersAccountInfoUpdate(authorsWithNoAvatars)
        }
    }


    fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            mEventsProvider?.requestEventsUpdate(mEventTypes?.toList(), limit = updateLimit)
        }
    }

    fun onFollowClick(eventListItem: EventListItem) {
        val event = eventListItem.golosEvent
        if (event is GolosSubscribeEvent) {
            mUsersProvider?.subscribeOnGolosUserBlog(event.fromUsers.firstOrNull() ?: return)
        } else {
            Timber.e("event must be of subscribe type, but got $event")
        }

    }

    private fun getSorter() = mEventsSorter

    fun onScrollToTheEnd() {
        mEventsProvider?.requestEventsUpdate(mEventTypes?.toList(),
                mEventsList.value?.events?.lastOrNull()?.golosEvent?.id, updateLimit) { _, _ -> (updateState as MutableLiveData<Boolean>).value = false }
        (updateState as MutableLiveData<Boolean>).value = true
    }

    fun onStart() {
        mEventsProvider?.let {
            mEventsList.addSource(it.getEvents(mEventTypes?.toList())) {
                onLiveDataChanged()
            }
        }
        mUsersProvider?.let {
            mEventsList.addSource(it.usersAvatars) {
                onLiveDataChanged()
            }
        }
        if (mEventTypes.orEmpty().contains(EventType.SUBSCRIBE)) {
            mEventsList.addSource(mUsersProvider?.currentUserSubscriptions
                    ?: return) { onLiveDataChanged() }
            mEventsList.addSource(mUsersProvider?.currentUserSubscriptionsUpdateStatus
                    ?: return) { onLiveDataChanged() }
        }
    }


    fun onStop() {
        mEventsProvider?.let { mEventsList.removeSource(it.getEvents(mEventTypes?.toList())) }
        mUsersProvider?.let { mEventsList.removeSource(it.usersAvatars) }

        mEventsList.removeSource(mUsersProvider?.currentUserSubscriptions ?: return)
        mEventsList.removeSource(mUsersProvider?.currentUserSubscriptionsUpdateStatus ?: return)
    }

    fun onEventClick(fragmentActivity: FragmentActivity, it: EventListItem) {
        if (it.golosEvent is PostLinkable) {
            (it.golosEvent as? PostLinkable)?.getLink()?.let {
                Timber.e("match it = $it")
                StoryActivity.start(fragmentActivity, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
            }
        } else if (it.golosEvent is Authorable) {
            val auth = it.golosEvent as Authorable
            UserProfileActivity.start(fragmentActivity, auth.getAuthors().firstOrNull() ?: return)
        }
    }

    fun requestUpdate() {
        onChangeVisibilityToUser(true)
    }


}