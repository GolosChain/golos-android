package io.golos.golos.screens.tags.model

import android.os.Parcel
import android.os.Parcelable
import io.golos.golos.repository.model.Tag
import io.golos.golos.utils.Translit

/**
 * Created by yuri on 08.01.18.
 */
class LocalizedTag(val tag: Tag) : Comparable<LocalizedTag>, Parcelable {
    private var mLocalizedName: String

    init {
        mLocalizedName = if (tag.name.startsWith("ru--"))
            Translit.lat2Ru(tag.name.removePrefix("ru--"))
        else tag.name
    }

    fun getLocalizedName(): String {
        return mLocalizedName
    }

    override fun compareTo(other: LocalizedTag): Int {
        return this.getLocalizedName().compareTo(other.getLocalizedName())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalizedTag) return false

        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }

    override fun toString(): String {
        return "LocalizedTag(tag=$tag, mLocalizedName='$mLocalizedName')"
    }

    constructor(source: Parcel) : this(
            source.readParcelable<Tag>(Tag::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(tag, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LocalizedTag> = object : Parcelable.Creator<LocalizedTag> {
            override fun createFromParcel(source: Parcel): LocalizedTag = LocalizedTag(source)
            override fun newArray(size: Int): Array<LocalizedTag?> = arrayOfNulls(size)
        }
    }
}

