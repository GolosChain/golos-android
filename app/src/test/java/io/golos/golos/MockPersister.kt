package io.golos.golos

import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.utils.toArrayList
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by yuri on 15.12.17.
 */
object MockPersister : Persister() {
    var users = HashMap<String, String>()
    var tags = ArrayList<Tag>()
    var userData: UserData? = null
    var name: String? = null
    var userSubscribedTags = ArrayList<Tag>()
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
        users.put(userName, "$avatarPath#$$#$updatedDate")

    }

    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
        if (!users.containsKey(userName)) return null
        val path = users.get(userName)!!.split("#$$#")
        return Pair(path[0].replace("#$$#", ""),
                 path[1].replace("#$$#", "").toLong())
    }

    override fun getActiveUserData(): UserData? = userData

    override fun saveUserData(userData: UserData) {
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