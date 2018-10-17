package io.golos.golos.repository.persistence

import android.content.Context
import android.util.Base64
import com.crashlytics.android.Crashlytics
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import io.fabric.sdk.android.Fabric
import io.golos.golos.App
import io.golos.golos.repository.GolosUsersPersister
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.*
import io.golos.golos.utils.mapper
import io.golos.golos.utils.toArrayList
import timber.log.Timber
import java.security.KeyStore
import java.security.KeyStoreException


/**
 * Created by yuri on 06.11.17.
 */
abstract class Persister : NotificationsPersister, GolosUsersPersister {

    abstract fun getCurrentUserName(): String?

    abstract fun saveCurrentUserName(name: String?)

    abstract fun getActiveUserData(): AppUserData?

    abstract fun saveUserData(userData: AppUserData)

    abstract fun saveTags(tags: List<Tag>)

    abstract fun saveBlogEntries(entries: List<GolosBlogEntry>)

    abstract fun getTags(): List<Tag>

    abstract fun getBlogEntries(): List<GolosBlogEntry>

    abstract fun saveUserSubscribedTags(tags: List<Tag>)

    abstract fun getUserSubscribedTags(): List<Tag>

    abstract fun deleteUserSubscribedTag(tag: Tag)

    abstract fun saveStories(stories: Map<StoryRequest, StoriesFeed>)

    abstract fun getStories(): Map<StoryRequest, StoriesFeed>

    abstract fun deleteAllStories()
    abstract fun deleteBlogEntries()

    companion
    object {
        private var mOnDevicePersister: Persister? = null
        val get: Persister
            @Synchronized get() {
                if (mOnDevicePersister == null) mOnDevicePersister = OnDevicePersister(App.context)
                return mOnDevicePersister!!
            }

    }

    abstract fun deleteUserData()
}

private class OnDevicePersister(private val context: Context) : Persister() {
    private val mDatabase: SqliteDb = SqliteDb(context)
    private val mPreference = context.getSharedPreferences("ondevicepersister", Context.MODE_PRIVATE)


    override fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {
        mDatabase.saveStories(stories)
    }

    override fun getStories(): Map<StoryRequest, StoriesFeed> {
        return mDatabase.getStories()
    }

    override fun saveBlogEntries(entries: List<GolosBlogEntry>) {
        mDatabase.saveBlogEntries(entries)
    }

    override fun getBlogEntries(): List<GolosBlogEntry> {
        return mDatabase.getBlogEntries()
    }

    override fun deleteBlogEntries() {
        mDatabase.deleteBlogEntries()
    }

    override fun deleteAllStories() {
        mDatabase.deleteAllStories()
    }

    override fun isUserSubscribedOnNotificationsThroughServices(): Boolean {
        return mPreference.getBoolean("setUserSubscribedOnNotificationsThroughServices", false)
    }


    override fun setUserSubscribedOnNotificationsThroughServices(isSubscribed: Boolean) {
        mPreference
                .edit()
                .putBoolean("setUserSubscribedOnNotificationsThroughServices", isSubscribed).apply()
    }

    override fun saveGolosUsersAccountInfo(list: List<GolosUserAccountInfo>) {
        mDatabase.saveGolosUsersAccountInfo(list)
    }

    override fun saveGolosUsersSubscribers(map: Map<String, List<String>>) {
        mDatabase.saveGolosUsersSubscribers(map)
    }

    override fun saveGolosUsersSubscriptions(map: Map<String, List<String>>) {
        mDatabase.saveGolosUsersSubscriptions(map)
    }

    override fun getGolosUsersAccountInfo(): List<GolosUserAccountInfo> {
        return mDatabase.getGolosUsersAccountInfo()
    }

    override fun getGolosUsersSubscribers(): Map<String, List<String>> {
        return mDatabase.getGolosUsersSubscribers()
    }

    override fun getGolosUsersSubscriptions(): Map<String, List<String>> {
        return mDatabase.getGolosUsersSubscriptions()
    }

    private fun saveKeys(keysToSave: Map<PrivateKeyType, String?>) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val encryptor = EnCryptor()
        keysToSave.forEach {
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
        }
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


    override fun getCurrentUserName(): String? {
        return getActiveUserData()?.userName
    }

    override fun saveCurrentUserName(name: String?) {
        mPreference.edit().putString("username", name).apply()
    }

    override fun getActiveUserData(): AppUserData? {
        val userDataString = mPreference.getString("userdata", "") ?: return null
        if (userDataString.isEmpty()) return null
        val userData = mapper.readValue(userDataString, AppUserData::class.java)
        try {
            val keys = getKeys(setOf(PrivateKeyType.ACTIVE, PrivateKeyType.POSTING))
            userData.privateActiveWif = keys[PrivateKeyType.ACTIVE]
            userData.privatePostingWif = keys[PrivateKeyType.POSTING]
            return userData
        } catch (e: Exception) {
            return null
        }
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
        mDatabase.deleteBlogEntries()
    }
}
