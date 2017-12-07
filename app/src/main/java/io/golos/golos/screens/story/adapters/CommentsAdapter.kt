package io.golos.golos.screens.story.adapters

import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.text.method.LinkMovementMethod
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
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.getVectorDrawable
import io.golos.golos.utils.toHtml
import timber.log.Timber
import java.util.*

/**
 * Created by yuri on 08.11.17.
 */
data class CommentHolderState(val comment: StoryWrapper,
                              val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                              val onAnswerClick: (RecyclerView.ViewHolder) -> Unit)

class CommentsAdapter(var onUpvoteClick: (StoryWrapper) -> Unit = { print(it) },
                      var onAnswerClick: (StoryWrapper) -> Unit = { print(it) }) : RecyclerView.Adapter<CommentViewHolder>() {

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

    override fun onBindViewHolder(holder: CommentViewHolder?, position: Int) {
        holder?.state = CommentHolderState(items[position],
                onUpvoteClick = { onUpvoteClick.invoke(items[it.adapterPosition]) },
                onAnswerClick = { onAnswerClick.invoke(items[it.adapterPosition]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CommentViewHolder {
        return CommentViewHolder(parent!!)
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

    init {
        mText.movementMethod = LinkMovementMethod.getInstance()
        mTimeTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_access_time_gray_24dp), null, null, null)
        mUpvoteBtn.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_upvote_18_gray), null, null, null)
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
                if (comment.avatarPath == null) mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                else {
                    val error = mGlide.load(R.drawable.ic_person_gray_24dp)
                    mGlide.load(comment.avatarPath)
                            .error(error)
                            .apply(RequestOptions().fitCenter().placeholder(R.drawable.ic_person_gray_24dp))
                            .into(mAvatar)
                }
                mUsernameTv.text = comment.author


               val currentTime = System.currentTimeMillis() - TimeZone.getDefault().getOffset(System.currentTimeMillis())

                val dif = currentTime - comment.created
                val hour = 1000 * 60 * 60
                val hoursAgo = dif / hour
                if  (hoursAgo == 0L){
                    mTimeTv.text =  itemView.resources.getString(R.string.less_then_hour_ago)
                }
                else if (hoursAgo < 24) {
                    mTimeTv.text = "$hoursAgo ${itemView.resources.getQuantityString(R.plurals.hours, hoursAgo.toInt())} ${itemView.resources.getString(R.string.ago)}"
                } else {
                    val daysAgo = hoursAgo / 24
                    mTimeTv.text = "$daysAgo ${itemView.resources.getQuantityString(R.plurals.days, daysAgo.toInt())} ${itemView.resources.getString(R.string.ago)}"
                }
                mUpvoteBtn.text = "$ ${String.format("%.2f", comment.payoutInDollars)}"

                if (comment.isUserUpvotedOnThis) {
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_triangle_in_circle_green_outline_20dp, 0, 0, 0)
                } else {
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorP))
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_triangle_in_cricle_gray_outline_20dp, 0, 0, 0)
                }
                if (field!!.comment.updatingState == UpdatingState.UPDATING) {
                    mUpvoteBtn.visibility = View.INVISIBLE
                    mUpvoteBtn.isClickable = false
                    mProgress.visibility = View.VISIBLE
                } else {
                    mUpvoteBtn.visibility = View.VISIBLE
                    mUpvoteBtn.isClickable = true
                    mProgress.visibility = View.GONE
                }
                val rows = ArrayList(StoryParserToRows().parse(comment))
                var imagePart = rows.findLast { it is ImageRow }
                if (imagePart != null) {
                    mImage.visibility = View.VISIBLE
                    val error = mGlide.load(R.drawable.error)
                    mGlide.load((imagePart as ImageRow).src)
                            .error(error)
                            .apply(RequestOptions().fitCenter().placeholder(R.drawable.error))
                            .into(mImage)
                    rows.remove(imagePart)
                } else {
                    mImage.setImageBitmap(null)
                    mImage.visibility = View.GONE
                }
                if (rows.size == 0) {
                    mText.visibility = View.GONE
                } else {
                    mText.visibility = View.VISIBLE
                    val outText = rows.map {
                        if (it is TextRow) "${it.text}\n"
                        else "<a href=\"${(it as ImageRow).src}\">${itemView.resources.getString(R.string.image)}</a>\n"
                    }.reduce { s1, s2 -> s1 + s2 }
                    mText.text = outText.trim().toHtml()
                }

            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_comment, parent, false)

    }
}