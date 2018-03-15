package io.golos.golos.screens.story.adapters

import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by yuri on 08.11.17.
 */
data class CommentHolderState(val comment: StoryWrapper,
                              val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                              val onDownVoteClick: (RecyclerView.ViewHolder) -> Unit,
                              val onAnswerClick: (RecyclerView.ViewHolder) -> Unit,
                              val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                              val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                              val onUserVotesClick: (RecyclerView.ViewHolder) -> Unit,
                              val onEditClick: (RecyclerView.ViewHolder) -> Unit)


class CommentsAdapter(var onUpvoteClick: (StoryWrapper) -> Unit = { print(it) },
                      var onDownVoteClick: (StoryWrapper) -> Unit = { print(it) },
                      var onAnswerClick: (StoryWrapper) -> Unit = { print(it) },
                      var onUserClick: (StoryWrapper) -> Unit = { print(it) },
                      var onCommentsClick: (StoryWrapper) -> Unit = { print(it) },
                      var onUserVotesClick: (StoryWrapper) -> Unit = { print(it) },
                      var onEditClick: (StoryWrapper) -> Unit = { print(it) }) : RecyclerView.Adapter<CommentViewHolder>() {


    var items = ArrayList<StoryWrapper>()
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition].story.id == value[newItemPosition].story.id
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = value.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == value[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            field = value
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


class CommentViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mGlide = Glide.with(itemView)
    private val mText: TextView = itemView.findViewById(R.id.text)
    private val mUsernameTv: TextView = itemView.findViewById(R.id.username_tv)
    private val mTimeTv: TextView = itemView.findViewById(R.id.time_tv)
    private val mUpvoteBtn: Button = itemView.findViewById(R.id.upvote_btn)
    private val mImage: ImageView = itemView.findViewById(R.id.image)
    private val mAnswerIbtn: Button = itemView.findViewById(R.id.answer_btn)
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)
    private val mLayout: LinearLayoutCompat = itemView.findViewById(R.id.content_lo)
    private val mRootLo: ConstraintLayout = itemView.findViewById(R.id.root_lo)
    private val mProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val mVotesIv: TextView = itemView.findViewById(R.id.votes_btn)
    private val mDotsBtn: View = itemView.findViewById(R.id.dots_btn)

    init {
        mText.movementMethod = GolosMovementMethod.instance
        mTimeTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_access_time_gray_24dp), null, null, null)
        mUpvoteBtn.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_upvote_18_gray), null, null, null)
        mVotesIv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_profile_outline_gray_16dp), null, null, null)
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
                mUpvoteBtn.text = "$ "
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
                mVotesIv.setOnClickListener { state?.onUserVotesClick?.invoke(this) }
                mDotsBtn.setOnClickListener {
                    val popup = ListPopupWindow(itemView.context)
                    popup.anchorView = mDotsBtn
                    val items = ArrayList<CommentListAdapter.CommentListAdapterItems>()
                    if (state?.comment?.isStoryEditable == true) {
                        items.add(CommentListAdapter.CommentListAdapterItems.EDIT)
                    }
                    if (state?.comment?.story?.userVotestatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                        items.add(CommentListAdapter.CommentListAdapterItems.FLAG_RED)
                    } else {
                        items.add(CommentListAdapter.CommentListAdapterItems.FLAG_GRAY)
                    }

                    popup.setAdapter(CommentListAdapter(itemView.context, items))
                    popup.setContentWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            140.0f,
                            itemView.context.resources.displayMetrics).toInt())
                    popup.setOnItemClickListener({ _, _, position, _ ->
                        val item = items[position]
                        when (item) {
                            CommentListAdapter.CommentListAdapterItems.FLAG_GRAY -> state?.onDownVoteClick?.invoke(this)
                            CommentListAdapter.CommentListAdapterItems.FLAG_RED -> state?.onDownVoteClick?.invoke(this)
                            CommentListAdapter.CommentListAdapterItems.EDIT -> state?.onEditClick?.invoke(this)
                        }
                        popup.dismiss()
                    })
                    popup.show()
                }

                if (comment.avatarPath == null) mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                else {
                    val error = mGlide.load(R.drawable.ic_person_gray_24dp)
                    mGlide.load(ImageUriResolver.resolveImageWithSize(comment.avatarPath
                            ?: "", wantedwidth = mAvatar.width))
                            .error(error)
                            .apply(RequestOptions().fitCenter().placeholder(R.drawable.ic_person_gray_24dp))
                            .into(mAvatar)
                }
                mUsernameTv.text = comment.author


                val currentTime = System.currentTimeMillis() - TimeZone.getDefault().getOffset(System.currentTimeMillis())

                val dif = currentTime - comment.created
                val hour = 1000 * 60 * 60
                val hoursAgo = dif / hour
                if (hoursAgo == 0L) {
                    mTimeTv.text = itemView.resources.getString(R.string.less_then_hour_ago)
                } else if (hoursAgo < 24) {
                    mTimeTv.text = "$hoursAgo ${itemView.resources.getQuantityString(R.plurals.hours, hoursAgo.toInt())} ${itemView.resources.getString(R.string.ago)}"
                } else {
                    val daysAgo = hoursAgo / 24
                    mTimeTv.text = "$daysAgo ${itemView.resources.getQuantityString(R.plurals.days, daysAgo.toInt())} ${itemView.resources.getString(R.string.ago)}"
                }
                mUpvoteBtn.text = "$ ${String.format("%.3f", comment.payoutInDollars)}"

                if (comment.userVotestatus == GolosDiscussionItem.UserVoteType.VOTED) {
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_triangle_in_circle_green_outline_20dp, 0, 0, 0)
                } else {
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorP))
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_triangle_in_cricle_gray_outline_20dp, 0, 0, 0)
                }

                if (field!!.comment.updatingState == UpdatingState.UPDATING) {
                    mUpvoteBtn.setViewGone()
                    mUpvoteBtn.isClickable = false
                    mProgress.setViewVisible()
                } else {
                    mUpvoteBtn.setViewVisible()
                    mUpvoteBtn.isClickable = true
                    mProgress.setViewGone()
                }
                mVotesIv.text = field?.comment?.story?.votesNum?.toString() ?: ""
                val rows = ArrayList(StoryParserToRows.parse(comment, true))
                var imagePart = rows.find { it is ImageRow }
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
                    }.reduce { s1, s2 -> s1 + s2 }
                    mText.text = outText.trim().toHtml().trim()
                }
            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_comment, parent, false)

    }
}