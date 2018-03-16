package io.golos.golos.repository.model

import com.google.common.primitives.Doubles

/**
 * Created by yuri on 17.01.18.
 */
data class VotedUserObject(val name: String,
                           var avatar: String?,
                           var gbgValue: Double) : Comparable<VotedUserObject> {
    override fun compareTo(other: VotedUserObject): Int {
        return Doubles.compare(other.gbgValue, this.gbgValue)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VotedUserObject) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
