package io.golos.golos.repository.model

import com.google.common.primitives.Doubles

/**
 * Created by yuri on 17.01.18.
 */
data class VotedUserObject(val name: String,
                           val percent: Short,
                           var gbgValue: Double) : Comparable<VotedUserObject> {
    override fun compareTo(other: VotedUserObject): Int {
        return Doubles.compare(other.gbgValue, this.gbgValue)
    }
}
