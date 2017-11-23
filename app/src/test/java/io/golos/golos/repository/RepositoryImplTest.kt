package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.Utils
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.screens.editor.EditorImagePart
import io.golos.golos.screens.editor.EditorTextPart
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor

/**
 * Created by yuri on 23.11.17.
 */
class RepositoryImplTest {
    val publicActive = "GLS7feysP2A87x4uNwn68q13rF3bchD6AKhJWf4CmPWjqQF8vCb5G"
    val privateActive = "5K7YbhJZqGnw3hYzsmH5HbDixWP5ByCBdnJxM5uoe9LuMX5rcZV"
    val publicPosting = "GLS75PefYywHJtG1cDtd4JoF7NRMT7tJig69zn1YytFdbsDHatLju"
    val privatePosting = "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3"
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val executor: Executor
        get() {
            return Executor { it.run() }
        }
    private lateinit var repo: RepositoryImpl
    @Before
    fun before() {
        repo = RepositoryImpl(
                executor,
                executor,
                object : Persister() {
                    var users = HashMap<String, String>()
                    var keys = HashMap<PrivateKeyType, String?>()
                    var name: String? = null
                    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
                        users.put(userName, avatarPath + "__" + updatedDate)

                    }

                    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
                        if (!users.containsKey(userName)) return null
                        val path = users.get(userName)!!.split("__")
                        return Pair(path[0], path[1].toLong())
                    }

                    override fun saveKeys(keys: Map<PrivateKeyType, String?>) {
                        this.keys = HashMap(keys)
                    }

                    override fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
                        val out = HashMap<PrivateKeyType, String?>()
                        keys.forEach { t, u ->
                            if (types.contains(t)) out[t] = u
                        }
                        return out
                    }

                    override fun getCurrentUserName() = name

                    override fun saveCurrentUserName(name: String?) {
                        this.name = name
                    }
                }, ApiImpl()
        )
    }

    @Test
    fun createPost() {
        repo.authWithActiveWif("yuri-vlad-second", privateActive)
        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        repo.createPost(UUID.randomUUID().toString(), listOf(EditorTextPart("sdg", "test content", pointerPosition = null),
                image),
                listOf("ase", "гаврик"), { a, b -> print(b) })

    }

}