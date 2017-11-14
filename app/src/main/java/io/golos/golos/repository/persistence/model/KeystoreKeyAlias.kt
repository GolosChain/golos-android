package io.golos.golos.repository.persistence.model

import eu.bittrade.libs.steemj.enums.PrivateKeyType

/**
 * Created by yuri on 10.11.17.
 */
enum class KeystoreKeyAlias {
    ACTIVE_WIF, POSTING_WIF, MEMO_WIF, OWNER_WIF


}

object KeystoreKeyAliasConverter {
    fun convert(keyType: PrivateKeyType): KeystoreKeyAlias {
        return when (keyType){
            PrivateKeyType.ACTIVE -> KeystoreKeyAlias.ACTIVE_WIF
            PrivateKeyType.POSTING -> KeystoreKeyAlias.POSTING_WIF
            PrivateKeyType.MEMO -> KeystoreKeyAlias.MEMO_WIF
            PrivateKeyType.OWNER -> KeystoreKeyAlias.OWNER_WIF
            else -> throw IllegalArgumentException("unknown keyType $keyType")
        }
    }
}