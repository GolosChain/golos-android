package io.golos.golos.screens.events

import android.arch.lifecycle.*
import android.support.v4.app.FragmentActivity
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.EventsProvider
import io.golos.golos.repository.GolosUsersRepository
import io.golos.golos.repository.StoriesProvider
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.services.*
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.utils.MainThreadExecutor
import io.golos.golos.utils.UpdatingState
import timber.log.Timber
import java.util.concurrent.Executors

data class EventsList(val events: List<EventListItem>)

class EventsViewModel : ViewModel() {
    private val mMainTreadExecutor = MainThreadExecutor()

    private val mEventsList = MediatorLiveData<EventsList>()
    val eventsList: LiveData<List<EventsListItemWrapper>> = Transformations.map(mEventsList) {
        getSorter().getListItems(it.events)
    }
    val updateState = MutableLiveData<Boolean>() as LiveData<Boolean>
    private lateinit var mEventsProvider: EventsProvider
    private var mEventTypes: Set<EventType>? = null
    private lateinit var mEventsSorter: EventsSorterUseCase
    private lateinit var mUsersProvider: GolosUsersRepository
    private lateinit var mUserDataProvider: UserDataProvider
    private lateinit var mStoriesProvider: StoriesProvider
    private val updateLimit = 15

    fun onCreate(eventTypes: List<EventType>?,
                 eventsProvider: EventsProvider,
                 eventsSorter: EventsSorterUseCase,
                 usersProvider: GolosUsersRepository,
                 userStatusProvider: UserDataProvider,
                 storiesProvider: StoriesProvider) {

        Timber.e("eventTypes $eventTypes")

        mEventsProvider = eventsProvider
        if (eventTypes != null) {
            val types = HashSet<EventType>()
            types.addAll(eventTypes)
            mEventTypes = types
        }

        mEventsSorter = eventsSorter
        mUsersProvider = usersProvider
        mUserDataProvider = userStatusProvider
        mStoriesProvider = storiesProvider
    }

    private fun onLiveDataChanged() {

        val events = mEventsProvider.getEvents(mEventTypes?.toList()).value.orEmpty()

        val avatars = mUsersProvider.usersAvatars.value.orEmpty()

        val out = ArrayList<EventListItem>(events.size)

        val stories = mStoriesProvider
                .getStories(FeedType.UNCLASSIFIED, null).value?.items.orEmpty()
                .mapNotNull { it.rootStory() }
                .associateBy { it.permlink }

        val currentUserSubscriptions = mUsersProvider.currentUserSubscriptions.value.orEmpty()
        val currentSubscriptionsProgress = mUsersProvider.currentUserSubscriptionsUpdateStatus.value.orEmpty()

        mExecutor.execute {
            events.forEach {
                var needToLoadDisussion = false
                var author: String = ""
                var permlink: String = ""
                val item: EventListItem = when (it) {
                    is GolosVoteEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.permlink
                        }
                        VoteEventListItem.create(it,
                                avatars[it.fromUsers.firstOrNull().orEmpty()],
                                discussion?.title.orEmpty())
                    }
                    is GolosFlagEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.permlink
                        }
                        FlagEventListItem.create(it,
                                avatars[it.fromUsers.firstOrNull().orEmpty()],
                                discussion?.title.orEmpty())
                    }
                    is GolosTransferEvent -> TransferEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                    is GolosSubscribeEvent -> {
                        val subscribeStatus: SubscribeStatus =
                                SubscribeStatus.create(currentUserSubscriptions.contains(it.fromUsers.firstOrNull().orEmpty())
                                        , currentSubscriptionsProgress[it.fromUsers.firstOrNull().orEmpty()]
                                        ?: UpdatingState.DONE)
                        val out = SubscribeEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()],

                                subscribeStatus, !currentUserSubscriptions.contains(it.fromUsers.firstOrNull().orEmpty()))
                        out
                    }
                    is GolosUnSubscribeEvent -> UnSubscribeEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                    is GolosReplyEvent -> {
                        val discussion = stories[it.parentPermlink]
                        if (discussion == null) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.parentPermlink
                        }
                        ReplyEventListItem.create(it,
                                avatars[it.fromUsers.firstOrNull().orEmpty()],
                                discussion?.title.orEmpty())
                    }
                    is GolosRepostEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null && it.counter == 1) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.permlink
                        }
                        RepostEventListItem.create(it,
                                avatars[it.fromUsers.firstOrNull().orEmpty()],
                                discussion?.title.orEmpty())
                    }
                    is GolosMentionEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null && it.counter == 1) {
                            needToLoadDisussion = true
                            author = it.fromUsers.firstOrNull().orEmpty()
                            permlink = it.permlink
                        }
                        MentionEventListItem.create(it,
                                avatars[it.fromUsers.firstOrNull().orEmpty()])
                    }
                    is GolosAwardEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.permlink
                        }
                        AwardEventListItem.create(it,
                                0.0f,
                                stories[it.permlink]?.title.orEmpty())
                    }
                    is GolosCuratorAwardEvent -> {
                        val discussion = stories[it.permlink]
                        if (discussion == null) {
                            needToLoadDisussion = true
                            author = mUserDataProvider.appUserData.value?.name.orEmpty()
                            permlink = it.permlink
                        }
                        CuratorAwardEventListItem.create(it,
                                0.0f,
                                stories[it.permlink]?.title.orEmpty())
                    }
                    is GolosMessageEvent -> MessageEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                    is GolosWitnessVoteEvent -> WitnessVoteEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                    is GolosWitnessCancelVoteEvent -> WitnessCancelVoteEventListItem.create(it, avatars[it.fromUsers.firstOrNull().orEmpty()])
                }
                if (needToLoadDisussion) mStoriesProvider.requestStoryUpdate(author, permlink, null, false, false, FeedType.UNCLASSIFIED)
                out.add(item)
            }
            mMainTreadExecutor.execute {
                mEventsList.value = EventsList(out)
            }

            if (events.isNotEmpty()) {
                val authorsWithNoAvatars = out
                        .filter { it.avatarPath == null && it.golosEvent is Authorable }
                        .map { (it.golosEvent as Authorable).getAuthors().firstOrNull() }
                        .distinct()
                        .filter {
                            !mUsersProvider.usersAvatars.value.orEmpty().containsKey(it)
                        }
                        .filterNotNull()

                if (authorsWithNoAvatars.isNotEmpty()) mUsersProvider.requestUsersAccountInfoUpdate(authorsWithNoAvatars)
            }
        }
    }


    fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            mEventsProvider.requestEventsUpdate(mEventTypes?.toList(), limit = updateLimit, fromId = null)
        }
    }

    fun onFollowClick(eventListItem: EventListItem) {
        val event = eventListItem.golosEvent
        if (event is GolosSubscribeEvent) {
            mUsersProvider.subscribeOnGolosUserBlog(event.fromUsers.firstOrNull() ?: return)
        } else {
            Timber.e("event must be of subscribe type, but got $event")
        }

    }

    private fun getSorter() = mEventsSorter

    fun onScrollToTheEnd() {
        mEventsProvider.requestEventsUpdate(mEventTypes?.toList(),
                mEventsList.value?.events?.lastOrNull()?.golosEvent?.id, updateLimit) { _, _ -> (updateState as MutableLiveData<Boolean>).value = false }
        (updateState as MutableLiveData<Boolean>).value = true
    }

    fun onStart() {

        mEventsProvider.let {
            mEventsList.addSource(it.getEvents(mEventTypes?.toList())) {
                onLiveDataChanged()
            }
        }
        mUsersProvider.let {
            mEventsList.addSource(it.usersAvatars) {
                onLiveDataChanged()
            }
        }
        if (mEventTypes.orEmpty().contains(EventType.SUBSCRIBE) || mEventTypes == null) {
            mEventsList.addSource(mUsersProvider.currentUserSubscriptions) { onLiveDataChanged() }
            mEventsList.addSource(mUsersProvider.currentUserSubscriptionsUpdateStatus) { onLiveDataChanged() }
        }
        mEventsList.addSource(mStoriesProvider.getStories(FeedType.UNCLASSIFIED, null)) { onLiveDataChanged() }
    }


    fun onStop() {
        mEventsProvider.let { mEventsList.removeSource(it.getEvents(mEventTypes?.toList())) }
        mUsersProvider.let { mEventsList.removeSource(it.usersAvatars) }

        mEventsList.removeSource(mUsersProvider.currentUserSubscriptions)
        mEventsList.removeSource(mUsersProvider.currentUserSubscriptionsUpdateStatus)
        mEventsList.removeSource(mStoriesProvider.getStories(FeedType.UNCLASSIFIED, null))
    }

    fun onEventClick(fragmentActivity: FragmentActivity, it: EventListItem) {
        if (it.golosEvent is PostLinkable) {
            (it.golosEvent as? PostLinkable)?.getLink()?.let {
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

    companion object {
        @JvmStatic
        private val mExecutor = Executors.newSingleThreadExecutor()
    }
}