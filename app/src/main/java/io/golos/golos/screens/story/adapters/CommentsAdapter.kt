package io.golos.golos.screens.story.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.SpanFactory
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.*


data class CommentHolderState(val comment: StoryWrapper,
                              val onUpvoteClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onDownVoteClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onAnswerClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onUserClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onCommentsClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onUserVotesClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onEditClick: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit)


class CommentsAdapter(var onUpvoteClick: (StoryWrapper) -> Unit = { print(it) },
                      var onDownVoteClick: (StoryWrapper) -> Unit = { print(it) },
                      var onAnswerClick: (StoryWrapper) -> Unit = { print(it) },
                      var onUserClick: (StoryWrapper) -> Unit = { print(it) },
                      var onCommentsClick: (StoryWrapper) -> Unit = { print(it) },
                      var onUserVotesClick: (StoryWrapper) -> Unit = { print(it) },
                      var onEditClick: (StoryWrapper) -> Unit = { print(it) }) : androidx.recyclerview.widget.RecyclerView.Adapter<CommentViewHolder>() {


    private val mHashes: HashMap<Long, Int> = HashMap()
    var items = ArrayList<StoryWrapper>()
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition].story.id == value[newItemPosition].story.id
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = value.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = items[oldItemPosition].story
                    val oldHashCode = if (mHashes.containsKey(oldItem.id)) mHashes[oldItem.id] else 0
                    return oldHashCode == value[newItemPosition].hashCode()
                }
            }).dispatchUpdatesTo(this)
            field = value
            field.forEach {
                mHashes[it.story.id] = it.hashCode()
            }
        }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.state = CommentHolderState(items[position],
                onUpvoteClick = { onUpvoteClick.invoke(items[it.adapterPosition]) },
                onAnswerClick = { onAnswerClick.invoke(items[it.adapterPosition]) },
                onUserClick = { onUserClick.invoke(items[it.adapterPosition]) },
                onCommentsClick = { onCommentsClick.invoke(items[it.adapterPosition]) },
                onUserVotesClick = { onUserVotesClick.invoke(items[it.adapterPosition]) },
                onDownVoteClick = { onDownVoteClick.invoke(items[it.adapterPosition]) },
                onEditClick = { onEditClick.invoke(items[it.adapterPosition]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(parent)
    }
}


class CommentViewHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(this.inflate(parent)), SpanFactory {
    override fun <T : Any?> produceOfType(type: Class<*>): T {
        return itemView.context.createGolosSpan(type)
    }

    private val mGlide = Glide.with(itemView)
    private val mText: TextView = itemView.findViewById(R.id.text)
    private val mUsernameTv: TextView = itemView.findViewById(R.id.username_tv)
    private val mTimeTv: TextView = itemView.findViewById(R.id.time_tv)
    private val mUpvoteBtn: ImageView = itemView.findViewById(R.id.footer_comment_upvote_ibtn)
    private val mUpvoteCounter: TextView = itemView.findViewById(R.id.footer_comment_upvote_btn)
    private val mMoneyTv: TextView = itemView.findViewById(R.id.footer_comment_money_tv)
    private val mImage: ImageView = itemView.findViewById(R.id.image)
    private val mAnswerIbtn: TextView = itemView.findViewById(R.id.answer_btn)
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)
    private val mRootLo: ConstraintLayout = itemView.findViewById(R.id.root_lo)
    private val mProgress: ProgressBar = itemView.findViewById(R.id.footer_comment_progress_upvote)
    private val mDotsBtn: View = itemView.findViewById(R.id.dots_btn)
    private var mLastAvatar: String? = null

    init {
        mText.movementMethod = GolosMovementMethod.instance
    }

    var state: CommentHolderState? = null
        set(value) {
            field = value
            if (field == null) {
                mAnswerIbtn.setOnClickListener(null)
                mUpvoteBtn.setOnClickListener(null)
                mText.text = ""
                mUsernameTv.text = ""
                mTimeTv.text = ""
                mMoneyTv.text = ""
                mUpvoteCounter.text = ""
                mImage.setImageBitmap(null)
                mAvatar.setImageBitmap(null)
                mProgress.visibility = View.GONE
                mRootLo.setPadding(0, 0, 0, 0)
            } else {
                val comment = state!!.comment.story
                var level = comment.level
                if (level > 6) level = 6
                mRootLo.setPadding((level * itemView.resources.getDimension(R.dimen.margin_material)).toInt(), 0, 0, 0)
                mUpvoteBtn.setOnClickListener({ state?.onUpvoteClick?.invoke(this) })
                mAnswerIbtn.setOnClickListener({ state?.onAnswerClick?.invoke(this) })
                mAvatar.setOnClickListener { state?.onUserClick?.invoke(this) }
                mText.setOnClickListener { state?.onCommentsClick?.invoke(this) }
                mImage.setOnClickListener { mText.callOnClick() }
                mUsernameTv.setOnClickListener { mAvatar.callOnClick() }
                mUpvoteCounter.setOnClickListener { state?.onUserVotesClick?.invoke(this) }
                mDotsBtn.setOnClickListener {
                    val popup = ListPopupWindow(itemView.context)
                    popup.anchorView = mDotsBtn
                    val items = ArrayList<CommentListAdapter.CommentListAdapterItems>()
                    if (state?.comment?.isStoryEditable == true) {
                        items.add(CommentListAdapter.CommentListAdapterItems.EDIT)
                    }
                    if (state?.comment?.voteStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                        items.add(CommentListAdapter.CommentListAdapterItems.DIZLIKED)
                    } else {
                        items.add(CommentListAdapter.CommentListAdapterItems.NOT_DIZLIKED)
                    }

                    popup.setAdapter(CommentListAdapter(itemView.context, items))
                    popup.setContentWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            160.0f,
                            itemView.context.resources.displayMetrics).toInt())
                    popup.setOnItemClickListener { _, _, position, _ ->
                        val item = items[position]
                        when (item) {
                            CommentListAdapter.CommentListAdapterItems.NOT_DIZLIKED -> state?.onDownVoteClick?.invoke(this)
                            CommentListAdapter.CommentListAdapterItems.DIZLIKED -> state?.onDownVoteClick?.invoke(this)
                            CommentListAdapter.CommentListAdapterItems.EDIT -> state?.onEditClick?.invoke(this)
                        }
                        popup.dismiss()
                    }
                    popup.show()
                }

                val avatarPath = state!!.comment.authorAccountInfo?.avatarPath

                if (avatarPath == null) {
                    mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                    mLastAvatar = null
                } else {
                    if (mLastAvatar != avatarPath) {
                        val error = mGlide.load(R.drawable.ic_person_gray_24dp)
                        mGlide.load(ImageUriResolver.resolveImageWithSize(avatarPath, wantedwidth = mAvatar.width))
                                .error(error)
                                .apply(RequestOptions().fitCenter().placeholder(R.drawable.ic_person_gray_24dp))
                                .into(mAvatar)
                        mLastAvatar = avatarPath
                    }
                }
                mUsernameTv.text = comment.author

                mTimeTv.text = createTimeLabel(comment.created, itemView.context)

                mMoneyTv.text = calculateShownReward(state!!.comment, ctx = itemView.context)

                if (state!!.comment.voteStatus == GolosDiscussionItem.UserVoteType.VOTED) {
                    mUpvoteBtn.setImageResource(R.drawable.ic_liked_20dp)
                } else {
                    mUpvoteBtn.setImageResource(R.drawable.ic_like_20dp)
                }

                if (field!!.comment.voteUpdatingState?.state == UpdatingState.UPDATING) {
                    mUpvoteBtn.setViewInvisible()
                    mUpvoteCounter.setViewInvisible()
                    mUpvoteBtn.isClickable = false
                    mProgress.setViewVisible()
                } else {
                    mUpvoteBtn.setViewVisible()
                    mUpvoteCounter.setViewVisible()
                    mUpvoteBtn.isClickable = true
                    mProgress.setViewGone()
                }
                mUpvoteCounter.text = comment.upvotesNum.toString()

                val rows = ArrayList(StoryParserToRows.parse(comment, true))
                val imagePart = rows.find { it is ImageRow }
                if (imagePart != null) {
                    mImage.visibility = View.VISIBLE
                    val src = (imagePart as ImageRow).src
                    val error = mGlide.load(R.drawable.error)
                    var size = mImage.width
                    if (size <= 0) size = itemView.context.resources.displayMetrics.widthPixels / 2
                    if (size <= 0) size = 768
                    mGlide.load(ImageUriResolver.resolveImageWithSize(src, size))
                            .error(error)
                            .apply(RequestOptions().fitCenter().placeholder(R.drawable.error))
                            .into(mImage)
                    rows.remove(imagePart)
                } else {
                    mImage.setImageBitmap(null)
                    mImage.setViewGone()
                }
                if (rows.size == 0) {
                    mText.setViewGone()
                } else {
                    mText.visibility = View.VISIBLE
                    val outText = rows.map {
                        if (it is TextRow) "${it.text}\n"
                        else "<a href=\"${(it as ImageRow).src}\">${itemView.resources.getString(R.string.image)}</a>\n"
                    }.reduce { s1, s2 -> s1 + "\n" + s2 }

                    mText.text = KnifeParser.fromHtml(outText.trim(), this)
                }
            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_comment, parent, false)

    }
}