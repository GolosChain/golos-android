package io.golos.golos.repository.persistence

import android.content.Context
import android.preference.PreferenceManager
import android.util.Base64
import com.crashlytics.android.Crashlytics
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.fabric.sdk.android.Fabric
import io.golos.golos.App
import io.golos.golos.repository.model.NotificationsPersister
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.repository.model.StoryRequest
import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.persistence.model.*
import io.golos.golos.utils.mapper
import io.golos.golos.utils.toArrayList
import timber.log.Timber
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.*
import kotlin.collections.HashMap


/**
 * Created by yuri on 06.11.17.
 */
abstract class Persister : NotificationsPersister {

    abstract fun saveAvatarPathForUser(userAvatar: UserAvatar)

    abstract fun saveAvatarsPathForUsers(userAvatars: List<UserAvatar>)

    abstract fun getAvatarForUser(userName: String): Pair<String, Long>?

    abstract fun getAvatarsFor(users: List<String>): Map<String, UserAvatar?>

    abstract fun getCurrentUserName(): String?

    abstract fun saveCurrentUserName(name: String?)

    abstract fun getActiveUserData(): AppUserData?

    abstract fun saveUserData(userData: AppUserData)

    abstract fun saveTags(tags: List<Tag>)

    abstract fun getTags(): List<Tag>

    abstract fun saveUserSubscribedTags(tags: List<Tag>)

    abstract fun getUserSubscribedTags(): List<Tag>

    abstract fun deleteUserSubscribedTag(tag: Tag)

    abstract fun saveStories(stories: Map<StoryRequest, StoriesFeed>)

    abstract fun getStories(): Map<StoryRequest, StoriesFeed>

    abstract fun deleteAllStories()

    companion
    object {
        private var mOnDevicePersister: Persister? = null
        val get: Persister
            @Synchronized get() {
                if (App.isMocked) return MockPersister() else {
                    if (mOnDevicePersister == null) mOnDevicePersister = OnDevicePersister(App.context)
                    return mOnDevicePersister!!
                }
            }
    }

    abstract fun deleteUserData()


}

private class OnDevicePersister(private val context: Context) : Persister() {
    private val mDatabase: SqliteDb = SqliteDb(context)
    private val mPreference = context.getSharedPreferences("ondevicepersister", Context.MODE_PRIVATE)

    override fun saveAvatarPathForUser(userAvatar: UserAvatar) {
        mDatabase.saveAvatar(userAvatar)
    }

    override fun saveAvatarsPathForUsers(userAvatars: List<UserAvatar>) {
        mDatabase.saveAvatars(userAvatars)
    }

    override fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {
        mDatabase.saveStories(stories)
    }

    override fun getStories(): Map<StoryRequest, StoriesFeed> {
        return mDatabase.getStories()
    }

    override fun deleteAllStories() {
        mDatabase.deleteAllStories()
    }

    private fun saveKeys(keysToSave: Map<PrivateKeyType, String?>) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val encryptor = EnCryptor()
        keysToSave.forEach({
            val keyAlias = KeystoreKeyAliasConverter.convert(it.key)
            if (it.value == null) {
                try {
                    if (keyStore.containsAlias(keyAlias.name))
                        keyStore.deleteEntry(keyAlias.name)
                } catch (e: KeyStoreException) {
                    Timber.e(e)
                    if (Fabric.isInitialized()) Crashlytics.logException(e)
                }

            } else {
                val stringToEncrypt = it.value!!
                val outBuffer = encryptor.encryptText(keyAlias, stringToEncrypt)
                mPreference.edit().putString(keyAlias.name, Base64.encodeToString(outBuffer, Base64.DEFAULT)).apply()
            }
        })
    }

    override fun saveUserSubscribedTags(tags: List<Tag>) {
        mDatabase.saveTagsForFilter(tags, "f1")
    }

    override fun getUserSubscribedTags(): List<Tag> {
        return mDatabase.getTagsForFilter("f1")
    }

    override fun deleteUserSubscribedTag(tag: Tag) {
        val tags = getUserSubscribedTags().toArrayList()
        tags.remove(tag)
        saveUserSubscribedTags(tags)
    }

    override fun saveTags(tags: List<Tag>) {
        mDatabase.saveTags(tags)
    }

    override fun getTags(): List<Tag> {
        return mDatabase.getTags()
    }

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

    override fun getAvatarsFor(users: List<String>): Map<String, UserAvatar?> {
        return mDatabase.getAvatars(users)
    }

    override fun getCurrentUserName(): String? {
        return getActiveUserData()?.userName
    }

    override fun saveCurrentUserName(name: String?) {
        mPreference.edit().putString("username", name).apply()
    }

    override fun saveSubscribedOnTopic(topic: String?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("topic", topic).apply()
    }

    override fun getSubscribeOnTopic(): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("topic", null)
    }

    override fun getActiveUserData(): AppUserData? {
        val userDataString = mPreference.getString("userdata", "") ?: return null
        if (userDataString.isEmpty()) return null
        val userData = mapper.readValue(userDataString, AppUserData::class.java)
        val keys = getKeys(setOf(PrivateKeyType.ACTIVE, PrivateKeyType.POSTING))
        userData.privateActiveWif = keys[PrivateKeyType.ACTIVE]
        userData.privatePostingWif = keys[PrivateKeyType.POSTING]
        return userData
    }

    override fun saveUserData(userData: AppUserData) {
        saveKeys(mapOf(Pair(PrivateKeyType.POSTING, userData.privatePostingWif),
                Pair(PrivateKeyType.ACTIVE, userData.privateActiveWif)))
        val copy = userData.clone() as AppUserData
        copy.privateActiveWif = null
        copy.privatePostingWif = null
        mPreference.edit().putString("userdata", mapper.writeValueAsString(copy)).commit()
    }

    override fun deleteUserData() {
        saveKeys(mapOf(Pair(PrivateKeyType.POSTING, null), Pair(PrivateKeyType.ACTIVE, null)))
        saveCurrentUserName(null)
        mPreference.edit().putString("userdata", "").apply()
    }
}


private class MockPersister : Persister() {
    private var userData: AppUserData? = null
    private var currentUserName: String? = null
    override fun saveAvatarPathForUser(userAvatar: UserAvatar) {

    }

    override fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {

    }

    override fun saveSubscribedOnTopic(topic: String?) {

    }

    override fun getSubscribeOnTopic(): String? {
        return ""
    }

    override fun getStories(): Map<StoryRequest, StoriesFeed> {
        return hashMapOf()
    }

    override fun deleteAllStories() {

    }

    override fun saveAvatarsPathForUsers(userAvatars: List<UserAvatar>) {

    }

    override fun getAvatarsFor(users: List<String>): Map<String, UserAvatar?> {
        return HashMap()
    }

    override fun saveUserSubscribedTags(tags: List<Tag>) {

    }

    override fun getUserSubscribedTags(): List<Tag> {
        return arrayListOf()
    }

    override fun deleteUserSubscribedTag(tag: Tag) {
    }

    override fun getAvatarForUser(userName: String): Pair<String, Long> {
        Thread.sleep(150)
        return Pair("https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg", Date().time)
    }

    override fun saveTags(tags: List<Tag>) {
    }

    override fun getTags(): List<Tag> {
        return listOf()
    }

    override fun getCurrentUserName(): String? {
        return currentUserName
    }

    override fun saveCurrentUserName(name: String?) {
        currentUserName = name
    }

    override fun getActiveUserData(): AppUserData? {
        return userData
    }

    override fun saveUserData(userData: AppUserData) {
        this.userData = userData
    }

    override fun deleteUserData() {
        userData = null
    }
}