package io.golos.golos.repository.persistence

import android.content.Context
import android.util.Base64
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.App
import io.golos.golos.repository.persistence.model.DeCryptor
import io.golos.golos.repository.persistence.model.EnCryptor
import io.golos.golos.repository.persistence.model.KeystoreKeyAliasConverter
import io.golos.golos.repository.persistence.model.UserAvatar
import java.security.KeyStore
import java.util.*
import kotlin.collections.HashMap


/**
 * Created by yuri on 06.11.17.
 */
 abstract class Persister {


    abstract fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long)

    abstract fun getAvatarForUser(userName: String): Pair<String, Long>?

    abstract fun saveKeys(keys: Map<PrivateKeyType, String?>)

    abstract fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?>

    abstract fun getCurrentUserName(): String?
    abstract fun saveCurrentUserName(name: String?)

    companion
    object {
        val get: Persister
            @Synchronized get() {
                if (App.isMocked) return MockPersister() else {
                    return OnDevicePersister(App.context)
                }
            }
    }

}

private class OnDevicePersister(private val context: Context) : Persister() {
    private val mDatabase: SqliteDb = SqliteDb(context)
    private val mPreference = context.getSharedPreferences("ondevicepersister", Context.MODE_PRIVATE)

    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
        mDatabase.saveAvatar(UserAvatar(userName, avatarPath, updatedDate))
    }

    override fun saveKeys(keysToSave: Map<PrivateKeyType, String?>) {
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
                mPreference.edit().putString("${keyAlias.name}_iv", Base64.encodeToString(encryptor.initializationBuffer, Base64.DEFAULT)).apply()
            }
        })
    }

    override fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val decryptor = DeCryptor()
        val out = HashMap<PrivateKeyType, String?>()

        types.forEach {
            val keyAlias = KeystoreKeyAliasConverter.convert(it)
            if (!keyStore.containsAlias(keyAlias.name)) out.put(it, null)
            else {
                val inBufferString = mPreference.getString(keyAlias.name, null)
                val inInitVectorString = mPreference.getString("${keyAlias.name}_iv", null)
                if (inBufferString == null || inInitVectorString == null) out.put(it, null)
                else {
                    val inBuffer = Base64.decode(inBufferString, Base64.DEFAULT)
                    val inInitVector = Base64.decode(inInitVectorString, Base64.DEFAULT)
                    out.put(it, decryptor.decryptData(keyAlias, inBuffer, inInitVector))
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
        return mPreference.getString("username", null)
    }

    override fun saveCurrentUserName(name: String?) {
        mPreference.edit().putString("username", name).apply()
    }
}


private class MockPersister : Persister() {
    private var keys = HashMap<PrivateKeyType, String?>()
    private var currentUserName: String? = null
    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {

    }

    override fun getAvatarForUser(userName: String): Pair<String, Long> {
        return Pair("https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg", Date().time)
    }

    override fun saveKeys(keys: Map<PrivateKeyType, String?>) {
        this.keys = HashMap(keys)
    }

    override fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
        return Collections.unmodifiableMap(keys)
    }

    override fun getCurrentUserName(): String? {
        return currentUserName
    }

    override fun saveCurrentUserName(name: String?) {
        currentUserName = name
    }
}