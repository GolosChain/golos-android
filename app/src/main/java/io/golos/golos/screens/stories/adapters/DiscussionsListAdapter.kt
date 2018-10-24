package io.golos.golos.screens.stories.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.golos.golos.R
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.screens.stories.adapters.viewholders.StoriesViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeCompactViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeFullViewHolder
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.stories.model.StoryWithCommentsClickListener
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.widgets.HolderClickListener
import io.golos.golos.utils.toArrayList

data class FeedCellSettings(val isFullSize: Boolean,
                            val isImagesShown: Boolean,
                            val nswfStrategy: NSFWStrategy,
                            val shownCurrency: UserSettingsRepository.GolosCurrency,
                            val bountyDisplay: UserSettingsRepository.GolosBountyDisplay)


data class StripeWrapper(val stripe: StoryWrapper,
                         val isImagesShown: Boolean,
                         val nswfStrategy: NSFWStrategy,
                         val feedCellSettings: FeedCellSettings)

class DiscussionsListAdapter(private var onCardClick: StoryWithCommentsClickListener,
                             private var onCommentsClick: StoryWithCommentsClickListener,
                             private var onUpvoteClick: StoryWithCommentsClickListener,
                             private var mOnDownVoteClick: StoryWithCommentsClickListener,
                             private var mOnReblogClick: StoryWithCommentsClickListener,
                             private var mRebloggedStoryAuthorlick: StoryWithCommentsClickListener,
                             private var onTagClick: StoryWithCommentsClickListener,
                             private var onUserClick: StoryWithCommentsClickListener,
                             private var onUpVotersClick: StoryWithCommentsClickListener,
                             private var onDownVotersClick: StoryWithCommentsClickListener,
                             feedCellSettings: FeedCellSettings)
    : androidx.recyclerview.widget.RecyclerView.Adapter<StoriesViewHolder>() {


    var feedCellSettings = feedCellSettings
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            } else {
                field = value
            }
        }
    private val mStripes = ArrayList<StoryWrapper>()

    fun setStripes(newItems: List<StoryWrapper>) {
        if (mStripes.isEmpty()) {
            mStripes.addAll(newItems)
            notifyDataSetChanged()

        } else {
            try {
                val old = mStripes.toArrayList()
                mStripes.clear()
                mStripes.addAll(newItems)
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition].story.id == newItems[newItemPosition].story.id
                    }

                    override fun getOldListSize() = old.size
                    override fun getNewListSize() = newItems.size
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition] == newItems[newItemPosition]
                    }
                })
                result.dispatchUpdatesTo(this)
            } catch (e: Exception) {
                e.printStackTrace()
                mStripes.clear()
                mStripes.addAll(newItems)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (!feedCellSettings.isFullSize) R.layout.vh_stripe_compact_size
        else R.layout.vh_stripe_full_size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        return if (viewType == R.layout.vh_stripe_full_size) StripeFullViewHolder(parent,
                onUpvoteClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUpvoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCardClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onCardClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCommentsClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onCommentsClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onBlogClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onTagClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onAvatarClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUserClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onUpVotersClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUpVotersClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onDownVotersClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onDownVotersClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onRebloggerClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        mRebloggedStoryAuthorlick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onDownVoteClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        mOnDownVoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onReblogClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        mOnReblogClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                })
        else StripeCompactViewHolder(parent,
                onUpvoteClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUpvoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCardClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onCardClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onBlogClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onTagClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onUserClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUserClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onDownVoteClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        mOnDownVoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onRebloggerClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        mRebloggedStoryAuthorlick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onUpVotersClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUpVotersClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onDowVotersClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onDownVotersClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onAvatarClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onUserClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                })
    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        holder.let {
            val wrapper = StripeWrapper(mStripes[position],
                    feedCellSettings.isImagesShown,
                    feedCellSettings.nswfStrategy,
                    feedCellSettings)
            it.state = wrapper
        }
    }

    private fun getStoryForPosition(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder): StoryWrapper? {
        val pos = holder.adapterPosition
        if (pos < 0) return null
        return if (pos < mStripes.size) return mStripes[pos] else null
    }

    override fun getItemCount() = mStripes.size

}


