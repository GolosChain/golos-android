package io.golos.golos

import io.golos.golos.repository.model.StoryRequest
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.UserAvatar
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.utils.toArrayList
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by yuri on 15.12.17.
 */
object MockPersister : Persister() {
    var users = ArrayList<UserAvatar>()
    var tags = ArrayList<Tag>()
    var userData: AppUserData? = null
    var name: String? = null
    var userSubscribedTags = ArrayList<Tag>()
    override fun saveAvatarPathForUser(userAvatar: UserAvatar) {
        users.add(userAvatar)

    }

    override fun saveSubscribedOnTopic(topic: String?) {

    }

    override fun getSubscribeOnTopic(): String?  = ""

    override fun saveAvatarsPathForUsers(userAvatars: List<UserAvatar>) {
        users.addAll(userAvatars)
    }

    override fun getAvatarForUser(userName: String): Pair<String, Long>? {

        return users.find{ it.userName == userName}?.let { Pair(it.avatarPath?:return null,it.dateUpdated) }

    }

    override fun getActiveUserData(): AppUserData? = userData

    override fun saveUserData(userData: AppUserData) {
        this.userData = userData
    }

    override fun deleteUserData() {
        userData = null
    }

    override fun getAvatarsFor(users: List<String>): Map<String, UserAvatar?> {
       return HashMap()
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStories(): Map<StoryRequest, StoriesFeed> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteAllStories() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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