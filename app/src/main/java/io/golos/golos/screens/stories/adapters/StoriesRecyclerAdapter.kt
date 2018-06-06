package io.golos.golos.screens.stories.adapters

import android.os.Handler
import android.support.annotation.NonNull
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.golos.golos.R
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.screens.stories.adapters.viewholders.StoriesViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeCompactViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeFullViewHolder
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.stories.model.StoryWithCommentsClickListener
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.widgets.HolderClickListener
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class FeedCellSettings(val isFullSize: Boolean,
                            val isImagesShown: Boolean,
                            val nswfStrategy: NSFWStrategy,
                            val shownCurrency: UserSettingsRepository.GolosCurrency,
                            val bountyDisplay: UserSettingsRepository.GolosBountyDisplay)


data class StripeWrapper(val stripe: StoryWithComments,
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
    : RecyclerView.Adapter<StoriesViewHolder>() {

    companion object {
        @JvmStatic
        @NonNull
        private val workingExecutor: Executor

        init {
            val namedThreadFactory =
                    ThreadFactoryBuilder().setNameFormat("stories recycler threads -%d").build()
            workingExecutor = Executors.newSingleThreadExecutor(namedThreadFactory)
        }
    }


    var feedCellSettings = feedCellSettings
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            } else {
                field = value
            }
        }
    private var mStripes = ArrayList<StoryWithComments>()
    private val mItemsMap = HashMap<Long, Int>()
    val handler = Handler()


    fun setStripesCustom(newItems: List<StoryWithComments>) {
        if (mStripes.isEmpty()) {
            handler.post {

                mStripes = ArrayList(newItems).clone() as ArrayList<StoryWithComments>
                notifyDataSetChanged()
                mStripes.forEach {
                    mItemsMap[it.rootStory()?.id ?: 0L] = it.hashCode()
                }
            }

        } else {
            workingExecutor.execute {
                try {

                    val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            if (mStripes == null
                                    || newItems == null
                                    || mStripes.isEmpty()
                                    || newItems.isEmpty()
                                    || mStripes.lastIndex < oldItemPosition) return false

                            return mStripes[oldItemPosition].rootStory()?.id == newItems[newItemPosition].rootStory()?.id
                        }

                        override fun getOldListSize() = mStripes.size
                        override fun getNewListSize() = newItems.size
                        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            if (mStripes == null
                                    || newItems == null
                                    || mStripes.isEmpty()
                                    || newItems.isEmpty()
                                    || mStripes.lastIndex < oldItemPosition
                                    || newItems.size < newItemPosition) return false
                            val oldHash = mItemsMap[mStripes[oldItemPosition].rootStory()?.id ?: 0L]
                            return oldHash == newItems[newItemPosition].storyWithState()?.hashCode()
                        }
                    })
                    handler.post {
                        result.dispatchUpdatesTo(this)
                        mStripes = ArrayList(newItems)
                        mStripes.forEach {
                            mItemsMap[it.rootStory()?.id
                                    ?: 0L] = it.storyWithState()?.hashCode() ?: 0
                        }
                    }
                } catch (e: Exception) {
                    handler.post {
                        e.printStackTrace()
                        mStripes = ArrayList(newItems)
                        notifyDataSetChanged()
                    }
                }
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
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onUpvoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCardClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onCardClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCommentsClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onCommentsClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onShareClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onShareClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onBlogClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onTagClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onUserClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onUserClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onVotersClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onVotersClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                })
        else StripeCompactViewHolder(parent,
                onUpvoteClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onUpvoteClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCardClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onCardClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onCommentsClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onCommentsClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },

                onBlogClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
                        onTagClick.onClick(getStoryForPosition(holder) ?: return)
                    }
                },
                onUserClick = object : HolderClickListener {
                    override fun onClick(holder: RecyclerView.ViewHolder) {
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

    private fun getStoryForPosition(holder: RecyclerView.ViewHolder): StoryWithComments? {
        val pos = holder.adapterPosition
        if (pos < 0) return null
        return if (pos < mStripes.size) return mStripes[pos] else null
    }

    override fun getItemCount() = mStripes.size

}


