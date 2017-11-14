package io.golos.golos.repository.persistence.model

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Created by yuri on 10.11.17.
 */


internal class EnCryptor {
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val ANDROID_KEY_STORE = "AndroidKeyStore"
    var initializationBuffer: ByteArray? = null


    fun encryptText(alias: KeystoreKeyAlias, textToEncrypt: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias))
        initializationBuffer = cipher.iv
        return cipher.doFinal(textToEncrypt.toByteArray(charset("UTF-8")))
    }

    private fun getSecretKey(alias: KeystoreKeyAlias): SecretKey {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, 10)
        var keyGenerator: KeyGenerator? = null
        try {
            keyGenerator =  KeyGenerator
                    .getInstance("AES", ANDROID_KEY_STORE)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            keyGenerator =  KeyGenerator
                    .getInstance("RSA", ANDROID_KEY_STORE)
        }
        catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            keyGenerator =  KeyGenerator
                    .getInstance("AESWRAP", ANDROID_KEY_STORE)
        }
        catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            keyGenerator =  KeyGenerator
                    .getInstance("ARC4", ANDROID_KEY_STORE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyGenerator!!.init(KeyGenParameterSpec.Builder(alias.name,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setKeyValidityStart(start.time)
                    .setKeyValidityEnd(end.time)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build())
        } else {
            keyGenerator!!.init(SecureRandom())

        }
        return keyGenerator.generateKey()
    }
}


class DeCryptor {

    private var keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)

    init {
        keyStore!!.load(null)

    }

    fun decryptData(alias: KeystoreKeyAlias, encryptedData: ByteArray, encryptionIv: ByteArray): String {

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, encryptionIv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), spec)

        return String(cipher.doFinal(encryptedData), Charset.forName("UTF-8"))
    }

    private fun getSecretKey(alias: KeystoreKeyAlias): SecretKey {
        return (keyStore!!.getEntry(alias.name, null) as KeyStore.SecretKeyEntry).secretKey
    }

    companion object {

        private val TRANSFORMATION = "AES/GCM/NoPadding"
        private val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}