package io.golos.golos.screens.stories.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.golos.golos.R
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.screens.stories.adapters.viewholders.StoriesViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeCompactViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeFullViewHolder
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.stories.model.StoryWithCommentsClickListener
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.widgets.HolderClickListener

data class FeedCellSettings(val isFullSize: Boolean,
                            val isImagesShown: Boolean,
                            val nswfStrategy: NSFWStrategy,
                            val shownCurrency: UserSettingsRepository.GolosCurrency,
                            val bountyDisplay: UserSettingsRepository.GolosBountyDisplay)


data class StripeWrapper(val stripe: StoryWrapper,
                         val isImagesShown: Boolean,
                         val nswfStrategy: NSFWStrategy,
                         val feedCellSettings: FeedCellSettings)

class StoriesRecyclerAdapter(private var onCardClick: StoryWithCommentsClickListener,
                             private var onCommentsClick: StoryWithCommentsClickListener,
                             private var onShareClick: StoryWithCommentsClickListener,
                             private var onUpvoteClick: StoryWithCommentsClickListener,
                             private var onTagClick: StoryWithCommentsClickListener,
                             private var onUserClick: StoryWithCommentsClickListener,
                             private var onVotersClick: StoryWithCommentsClickListener,
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
    private val mItemsMap = HashMap<Long, Int>()


    fun setStripes(newItems: List<StoryWrapper>) {
        if (mStripes.isEmpty()) {
            mStripes.addAll(newItems)
            notifyDataSetChanged()

        } else {
            try {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return mStripes[oldItemPosition].story.id == newItems[newItemPosition].story.id
                    }

                    override fun getOldListSize() = mStripes.size
                    override fun getNewListSize() = newItems.size
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return mStripes[oldItemPosition] == newItems[newItemPosition]
                    }
                })
                mStripes.clear()
                mStripes.addAll(newItems)
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
                onShareClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onShareClick.onClick(getStoryForPosition(holder) ?: return)
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
                onVotersClick = object : HolderClickListener {
                    override fun onClick(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                        onVotersClick.onClick(getStoryForPosition(holder) ?: return)
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
                onUserClick = object : HolderClickListener {
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


