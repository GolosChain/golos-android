package io.golos.golos

import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.UserData
import java.util.*

/**
 * Created by yuri on 15.12.17.
 */
object MockPersister : Persister() {
    var users = HashMap<String, String>()
    var userData: UserData? = null
    var name: String? = null
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
        users.put(userName, avatarPath + "__" + updatedDate)

    }

    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
        if (!users.containsKey(userName)) return null
        val path = users.get(userName)!!.split("__")
        return Pair(path[0].replace("__", ""),
                path[1].replace("__", "").toLong())
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

}