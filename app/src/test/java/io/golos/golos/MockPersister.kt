package io.golos.golos

import io.golos.golos.repository.model.GolosBlogEntry
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.repository.model.StoryRequest
import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.repository.persistence.model.UserAvatar
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 15.12.17.
 */
object MockPersister : Persister() {
    var users = ArrayList<UserAvatar>()
    var tags = ArrayList<Tag>()
    var userData: AppUserData? = null
    var name: String? = null
    var userSubscribedTags = ArrayList<Tag>()
    private var isUserSubscribedOnNotificationsThroughServices = false
    private val mBlogEntries = ArrayList<GolosBlogEntry>()
    private val mUserAccInfo = ArrayList<GolosUserAccountInfo>()
    private val mUsersSubscribers = HashMap<String, List<String>>()
    private val mUsersSubscriptions = HashMap<String, List<String>>()


    override fun isUserSubscribedOnNotificationsThroughServices(): Boolean = isUserSubscribedOnNotificationsThroughServices

    override fun setUserSubscribedOnNotificationsThroughServices(isSubscribed: Boolean) {
        isUserSubscribedOnNotificationsThroughServices = isSubscribed
    }

    override fun saveBlogEntries(entries: List<GolosBlogEntry>) {
        mBlogEntries.addAll(entries)
    }

    override fun getBlogEntries(): List<GolosBlogEntry> {
        return emptyList()
    }

    override fun deleteBlogEntries() {
        mBlogEntries.clear()
    }

    override fun saveGolosUsersAccountInfo(list: List<GolosUserAccountInfo>) {
        mUserAccInfo.addAll(list)
    }

    override fun saveGolosUsersSubscribers(map: Map<String, List<String>>) {
        mUsersSubscribers.putAll(map)
    }

    override fun saveGolosUsersSubscriptions(map: Map<String, List<String>>) {
        mUsersSubscriptions.putAll(map)
    }

    override fun getGolosUsersAccountInfo(): List<GolosUserAccountInfo> {
        return mUserAccInfo
    }

    override fun getGolosUsersSubscribers(): Map<String, List<String>> {
        return mUsersSubscribers
    }

    override fun getGolosUsersSubscriptions(): Map<String, List<String>> {
        return mUsersSubscriptions
    }

    override fun getActiveUserData(): AppUserData? = userData

    override fun saveUserData(userData: AppUserData) {
        this.userData = userData
    }

    override fun deleteUserData() {
        userData = null
    }

    override fun getCurrentUserName() = userData?.userName

    override fun saveCurrentUserName(name: String?) {
        this.name = name
    }

    override fun saveTags(tags: List<Tag>) {
        this.tags = tags.toArrayList()
    }

    override fun getTags(): List<Tag> {
        return tags
    }

    override fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {

    }

    override fun getStories(): Map<StoryRequest, StoriesFeed> {
        return emptyMap()
    }

    override fun deleteAllStories() {
    }

    override fun saveUserSubscribedTags(tags: List<Tag>) {
        userSubscribedTags = ArrayList(tags)
    }

    override fun getUserSubscribedTags(): List<Tag> {
        return userSubscribedTags
    }

    override fun deleteUserSubscribedTag(tag: Tag) {
        userSubscribedTags.remove(tag)
    }
}