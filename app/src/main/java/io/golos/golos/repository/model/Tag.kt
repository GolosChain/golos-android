package io.golos.golos.repository.model

import android.os.Parcel
import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import eu.bittrade.libs.golosj.base.models.TrendingTag

/**
 * Created by yuri on 05.01.18.
 */
class Tag(
        @JsonProperty("name")
        val name: String,
        @JsonProperty("payoutInGbg")
        val payoutInGbg: Double,
        @JsonProperty("votes")
        val votes: Long,
        @JsonProperty("topPostsCount")
        val topPostsCount: Long) : Parcelable {
    constructor(tag: TrendingTag) : this(
            tag.name,
            tag.totalPayouts?.amount ?: 0.0,
            tag.netVotes,
            tag.topPosts)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "Tag(name='$name', payoutInGbg=$payoutInGbg, votes=$votes, topPostsCount=$topPostsCount)"
    }

    constructor(source: Parcel) : this(
            source.readString(),
            source.readDouble(),
            source.readLong(),
            source.readLong()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeDouble(payoutInGbg)
        writeLong(votes)
        writeLong(topPostsCount)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Tag> = object : Parcelable.Creator<Tag> {
            override fun createFromParcel(source: Parcel): Tag = Tag(source)
            override fun newArray(size: Int): Array<Tag?> = arrayOfNulls(size)
        }
    }
}