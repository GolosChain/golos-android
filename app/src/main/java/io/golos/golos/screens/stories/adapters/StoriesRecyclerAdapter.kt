package io.golos.golos.screens.stories.adapters

import android.os.Handler
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.stories.adapters.viewholders.StripeCompactViewHolder
import io.golos.golos.screens.stories.adapters.viewholders.StripeFullViewHolder
import io.golos.golos.screens.story.model.StoryWithComments
import java.util.concurrent.Executors

data class FeedCellSettings(val isFullSize: Boolean = true,
                            val isImagesShown: Boolean = true)


data class StripeWrapper(val stripe: StoryWithComments,
                         val isImagesShown: Boolean,
                         val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                         val onShareClick: (RecyclerView.ViewHolder) -> Unit,
                         val onBlogClick: (RecyclerView.ViewHolder) -> Unit,
                         val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                         val onVotersClick: (RecyclerView.ViewHolder) -> Unit)

class StoriesRecyclerAdapter(private var onCardClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onCommentsClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onShareClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onUpvoteClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onTagClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onUserClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onVotersClick: (StoryWithComments) -> Unit = { print(it) },
                             feedCellSettings:FeedCellSettings = FeedCellSettings())
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        @JvmStatic
        private val workingExecutor = Executors.newSingleThreadExecutor()
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
    private val handler = Handler()


    fun setStripesCustom(newItems: List<StoryWithComments>) {
        if (mStripes.isEmpty()) {
            handler.post {
                mStripes = ArrayList(newItems).clone() as ArrayList<StoryWithComments>
                notifyDataSetChanged()
                mStripes.forEach {
                    mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
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
                                    || mStripes.lastIndex < oldItemPosition
                                    || newItems.size < newItemPosition) return false
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
                            return oldHash == newItems[newItemPosition].rootStory()?.hashCode()
                        }
                    })
                    handler.post {
                        result.dispatchUpdatesTo(this)
                        mStripes = ArrayList(newItems)
                        mStripes.forEach {
                            mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == R.layout.vh_stripe_full_size) StripeFullViewHolder(parent)
        else StripeCompactViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        holder?.let {
            val wrapper = StripeWrapper(mStripes[position],
                    feedCellSettings.isImagesShown,
                    onUpvoteClick = { onUpvoteClick.invoke(mStripes[it.adapterPosition]) },
                    onCardClick = { onCardClick.invoke(mStripes[it.adapterPosition]) },
                    onCommentsClick = { onCommentsClick.invoke(mStripes[it.adapterPosition]) },
                    onShareClick = { onShareClick.invoke(mStripes[it.adapterPosition]) },
                    onBlogClick = { onTagClick.invoke(mStripes[it.adapterPosition]) },
                    onUserClick = { onUserClick.invoke(mStripes[it.adapterPosition]) },
                    onVotersClick = { onVotersClick.invoke(mStripes[it.adapterPosition]) })
            when (it) {
                is StripeFullViewHolder -> it.state = wrapper
                is StripeCompactViewHolder -> it.state = wrapper
                else -> {
                }
            }
        }
    }

    override fun getItemCount() = mStripes.size
}


