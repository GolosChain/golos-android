package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.golos.golos.repository.persistence.model.UserAvatar
import io.golos.golos.utils.MainThreadExecutor
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface AvatarRepository {
    val avatars: LiveData<Map<String, String?>>

    fun requestAvatarsUpdate(forUsers: List<String>)

}

interface AvatarPersister {
    fun saveAvatarPathForUser(userAvatar: UserAvatar)

    fun saveAvatarsPathForUsers(userAvatars: List<UserAvatar>)

    fun getAvatarForUser(userName: String): Pair<String, Long>?

    fun getAvatarsFor(users: List<String>): Map<String, UserAvatar?>

    fun getAllAvatars(): List<UserAvatar>
}

interface AvatarsApi {
    fun getUserAvatars(names: List<String>): Map<String, String?>
}

class AvatarRepositoryImpl(private val persister: AvatarPersister,
                           private val api: AvatarsApi,
                           private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
                           private val mainThreadExecutor: Executor = MainThreadExecutor(),
                           private val mAvatarRefreshDelay: Long = TimeUnit.DAYS.toMillis(7)) : AvatarRepository {
    private val mAvatars = MutableLiveData<Map<String, String?>>()


    fun setUp() {
        workerExecutor.execute {
            val avatars = persister.getAllAvatars()
            val currentTime = System.currentTimeMillis()
            val map = HashMap<String, String?>(avatars.size)
            avatars.forEach {
                val userName = it.userName
                if (currentTime < (it.dateUpdated + mAvatarRefreshDelay)) {
                    map[userName] = it.avatarPath
                }
            }
            mainThreadExecutor.execute {
                mAvatars.value = map
            }
        }

    }

    override val avatars: LiveData<Map<String, String?>>
        get() = mAvatars

    override fun requestAvatarsUpdate(forUsers: List<String>) {
        if (forUsers.isEmpty()) return

        workerExecutor.execute {
            val userList = forUsers.toSet().toList()
            api.getUserAvatars(userList
                    .filter { !mAvatars.value.orEmpty().containsKey(it) })
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        val all = HashMap(it)
                        all.putAll(mAvatars.value.orEmpty())
                        mainThreadExecutor.execute {
                            mAvatars.value = all
                        }
                        persister.saveAvatarsPathForUsers(it.map { UserAvatar(it.key, it.value, System.currentTimeMillis()) })
                    }
        }
    }
}