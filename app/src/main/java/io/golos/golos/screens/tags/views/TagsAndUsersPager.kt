package io.golos.golos.screens.tags.views

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.screens.tags.fragments.TagsListFragment
import io.golos.golos.screens.tags.fragments.UserListFragment
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.StringSupplier

class TagsAndUsersPager @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    private val mTagsListFragment: TagsListFragment
    private val mUserListFragment: UserListFragment

    init {
        val supportActivity = context as? AppCompatActivity
        if (supportActivity != null) {
            mTagsListFragment = TagsListFragment()
            mUserListFragment = UserListFragment()
            val adapter = Adapter(supportActivity.supportFragmentManager,
                    mTagsListFragment,
                    mUserListFragment,
                    object : StringSupplier {
                        override fun get(resId: Int, args: String?): String {
                            return context.getString(resId, args)
                        }
                    })
            offscreenPageLimit = 2
            setAdapter(adapter)
        } else {
            throw IllegalStateException("widget must be user only with AppCompatActivity")
        }

    }

    public var tags: List<LocalizedTag> by mTagsListFragment

    public var users: List<GolosUser> by mUserListFragment

    public var tagClickListener: OnTagClickListener?
        set(value) {
            mTagsListFragment.onTagClickListener = value
        }
        get() {
            return mTagsListFragment.onTagClickListener
        }

    public var userClickListener: OnUserClickListener?
        set(value) {
            mUserListFragment.userClickListener = value
        }
        get() {
            return mUserListFragment.userClickListener
        }


    class Adapter(fm: FragmentManager,
                  val tagsListFragment: TagsListFragment,
                  val userListFragment: UserListFragment,
                  val stringSupplier: StringSupplier) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> tagsListFragment
                else -> userListFragment
            }
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> stringSupplier.get(R.string.tags)
                else -> stringSupplier.get(R.string.users)
            }
        }
    }

    interface OnTagClickListener {
        fun onClick(tag: LocalizedTag)
    }

    interface OnUserClickListener {
        fun onClick(tag: GolosUser)
    }
}

