package io.golos.golos.persistence

import io.golos.golos.App
import io.golos.golos.persistence.model.UserAvatar
import java.util.*

/**
 * Created by yuri on 06.11.17.
 */
abstract class Persister {


    abstract fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long)

    abstract fun getAvatarForUser(userName: String): Pair<String, Long>?

    companion object {
        private var mSqliteDb: SqliteDb? = null
        val get: Persister
            @Synchronized get() {
                if (App.isMocked) return MockPersister() else {
                    if (mSqliteDb == null) mSqliteDb = SqliteDb(App.context)
                    return SQlitePersister(mSqliteDb!!)
                }
            }
    }

}

private class SQlitePersister(private val mDatabase: SqliteDb) : Persister() {
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
        mDatabase.saveAvatar(UserAvatar(userName, avatarPath, updatedDate))
    }

    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
        val userAvatar = mDatabase.getAvatar(userName)
        if (userAvatar?.avatarPath == null) return null
        else return Pair(userAvatar.avatarPath, userAvatar.dateUpdated)
    }
}


private class MockPersister : Persister() {
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {

    }

    override fun getAvatarForUser(userName: String): Pair<String, Long> {
        return Pair("https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg", Date().time)
    }
}