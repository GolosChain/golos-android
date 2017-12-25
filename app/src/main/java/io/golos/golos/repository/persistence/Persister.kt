package io.golos.golos.repository.persistence

import android.content.Context
import android.util.Base64
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.App
import io.golos.golos.repository.model.mapper
import io.golos.golos.repository.persistence.model.*
import io.golos.golos.screens.story.model.StoryTree
import java.security.KeyStore
import java.util.*
import kotlin.collections.HashMap


/**
 * Created by yuri on 06.11.17.
 */
abstract class Persister {


    abstract fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long)

    abstract fun getAvatarForUser(userName: String): Pair<String, Long>?

    abstract fun getCurrentUserName(): String?

    abstract fun saveCurrentUserName(name: String?)

    abstract fun getActiveUserData(): UserData?

    abstract fun saveUserData(userData: UserData)

   /* abstract fun saveStory(story: StoryTree)

    abstract fun getStory(id: Long): StoryTree?*/

    companion
    object {
        val get: Persister
            @Synchronized get() {
                if (App.isMocked) return MockPersister() else {
                    return OnDevicePersister(App.context)
                }
            }
    }

    abstract fun deleteUserData()


}

private class OnDevicePersister(private val context: Context) : Persister() {
    private val mDatabase: SqliteDb = SqliteDb(context)
    private val mPreference = context.getSharedPreferences("ondevicepersister", Context.MODE_PRIVATE)

    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
        mDatabase.saveAvatar(UserAvatar(userName, avatarPath, updatedDate))
    }

    private fun saveKeys(keysToSave: Map<PrivateKeyType, String?>) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val encryptor = EnCryptor()
        keysToSave.forEach({
            val keyAlias = KeystoreKeyAliasConverter.convert(it.key)
            if (it.value == null) keyStore.deleteEntry(keyAlias.name)
            else {
                val stringToEncrypt = it.value!!
                val outBuffer = encryptor.encryptText(keyAlias, stringToEncrypt)
                mPreference.edit().putString(keyAlias.name, Base64.encodeToString(outBuffer, Base64.DEFAULT)).apply()
            }
        })
    }

   /* override fun saveStory(story: StoryTree) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStory(id: Long): StoryTree? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/

    private fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val decryptor = DeCryptor()
        val out = HashMap<PrivateKeyType, String?>()

        types.forEach {
            val keyAlias = KeystoreKeyAliasConverter.convert(it)
            if (!keyStore.containsAlias(keyAlias.name)) out.put(it, null)
            else {
                val inBufferString = mPreference.getString(keyAlias.name, null)
                if (inBufferString == null) out.put(it, null)
                else {
                    val inBuffer = Base64.decode(inBufferString, Base64.DEFAULT)
                    out.put(it, decryptor.decryptData(keyAlias, inBuffer/*, inInitVector*/))
                }
            }
        }
        return out
    }

    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
        val userAvatar = mDatabase.getAvatar(userName)
        if (userAvatar?.avatarPath == null) return null
        else return Pair(userAvatar.avatarPath, userAvatar.dateUpdated)
    }

    override fun getCurrentUserName(): String? {
        return getActiveUserData()?.userName
    }

    override fun saveCurrentUserName(name: String?) {
        mPreference.edit().putString("username", name).apply()
    }

    override fun getActiveUserData(): UserData? {
        val userDataString = mPreference.getString("userdata", "") ?: return null
        if (userDataString.isEmpty()) return null
        val userData = mapper.readValue(userDataString, UserData::class.java)
        val keys = getKeys(setOf(PrivateKeyType.ACTIVE, PrivateKeyType.POSTING))
        userData.privateActiveWif = keys[PrivateKeyType.ACTIVE]
        userData.privatePostingWif = keys[PrivateKeyType.POSTING]
        return userData
    }

    override fun saveUserData(userData: UserData) {
        saveKeys(mapOf(Pair(PrivateKeyType.POSTING, userData.privatePostingWif),
                Pair(PrivateKeyType.ACTIVE, userData.privateActiveWif)))
        val copy = userData.clone() as UserData
        copy.privateActiveWif = null
        copy.privatePostingWif = null
        mPreference.edit().putString("userdata", mapper.writeValueAsString(copy)).apply()
    }

    override fun deleteUserData() {
        saveKeys(mapOf(Pair(PrivateKeyType.POSTING, null), Pair(PrivateKeyType.ACTIVE, null)))
        saveCurrentUserName(null)
        mPreference.edit().putString("userdata", "").apply()
    }
}


private class MockPersister : Persister() {
    private var userData: UserData? = null
    private var currentUserName: String? = null
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {

    }

    override fun getAvatarForUser(userName: String): Pair<String, Long> {
        Thread.sleep(150)
        return Pair("https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg", Date().time)
    }


    override fun getCurrentUserName(): String? {
        return currentUserName
    }

    override fun saveCurrentUserName(name: String?) {
        currentUserName = name
    }

    override fun getActiveUserData(): UserData? {
        return userData
    }

    override fun saveUserData(userData: UserData) {
        this.userData = userData
    }

    override fun deleteUserData() {
        userData = null
    }
}