package io.golos.golos.notifications

import eu.bittrade.libs.golosj.DatabaseMethods
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.base.models.Permlink
import eu.bittrade.libs.golosj.base.models.SignedTransaction
import eu.bittrade.libs.golosj.base.models.TimePointSec
import eu.bittrade.libs.golosj.base.models.operations.VoteOperation

interface SecretSigner {
    fun sign(user: String, secret: String): String
}

class GolosSecretSigner(
        private val databaseMethods: DatabaseMethods
) : SecretSigner {
    override fun sign(user: String, secret: String): String {
        val voteOperation = VoteOperation(AccountName(user),
                AccountName("test"),
                Permlink(secret),
                1.toShort())

        val transaction = SignedTransaction(3367,
                879276768,
                TimePointSec(1530888744000),
                listOf(voteOperation), listOf())

        return transaction.createSignWithoutCanonicalTest()
    }
}