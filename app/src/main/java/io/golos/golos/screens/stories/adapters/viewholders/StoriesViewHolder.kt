package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.*

/**
 * Created by yuri on 19.02.18.
 */
abstract class StoriesViewHolder(resId: Int,
                                 parent: ViewGroup) : GolosViewHolder(resId, parent) {

    private val mGlide = Glide.with(parent.context)
    private var oldAvatar: String? = null


    var state: StripeWrapper? = null
        set(value) {
            setUpTheme()
            handleImagePlacing(value, getMainImage())
            handlerStateChange(value, field)
            handleAvatarLoading(value, field)
            field = value
        }

    init {
        if (StripeFullViewHolder.noAvatarDrawable == null) StripeFullViewHolder.noAvatarDrawable = itemView.getVectorDrawable(R.drawable.ic_person_gray_32dp)
    }

    protected open fun handlerStateChange(newState: StripeWrapper?,
                                          oldState: StripeWrapper?) {
        val story = newState?.stripe?.story
        val wrapper = newState?.stripe

        if (newState == null || wrapper == null || story == null) return

        if (story.categoryName.startsWith("ru--")) {
            getBlogNameTv().text = Translit.lat2Ru(story.categoryName.substring(4))
        } else {
            getBlogNameTv().text = story.categoryName
        }


        getDateStampText().text = createTimeLabel(story.created, itemView.context)


        val shownName = newState.stripe.authorAccountInfo?.shownName.orEmpty().trim()
        if (shownName.isEmpty()) getShownUserNameTv().setViewGone()
        else {
            getShownUserNameTv().setViewVisible()
            getShownUserNameTv().text = shownName
        }
        getUserNickTv().text = story.author

        if (story.rebloggedBy.isNotEmpty()) {
            getReblogedByTv().setViewVisible()
            getReblogedByTv().text = story.rebloggedBy
        } else {
            getReblogedByTv().setViewGone()
        }

        if (story.title.length > 2) {
            getTitleTv().text = story.title.capitalize()
        } else {
            getTitleTv().text = ""
        }
        val bountyDisplay = newState.feedCellSettings.bountyDisplay
        getCommentsTv()?.text = story.commentsCount.toString()
        getUpvoteText().text = story.upvotesNum.toString()
        getDownVoteText().text = story.downvotesNum.toString()
        getMoneyText().text = calculateShownReward(newState.stripe,
                newState.feedCellSettings.shownCurrency,
                bountyDisplay,
                itemView.context)
        if (wrapper.voteStatus == GolosDiscussionItem.UserVoteType.VOTED) {
            if (getUpvoteIv().tag != "blue") {
                getUpvoteIv().setImageResource(R.drawable.ic_liked_20dp)
                getUpvoteIv().tag = "blue"
            }
        } else {
            if (getUpvoteIv().tag != "not_blue") {
                getUpvoteIv().setImageResource(R.drawable.ic_like_20dp)
                getUpvoteIv().tag = "not_blue"
            }
        }

        if (wrapper.voteStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
            if (getDownvoteIv().tag != "red") {
                getDownvoteIv().setImageResource(R.drawable.ic_dizliked_20dp)
                getDownvoteIv().tag = "red"
            }
        } else {
            if (getDownvoteIv().tag != "not_red") {
                getDownvoteIv().setImageResource(R.drawable.ic_dizlike_20dp)
                getDownvoteIv().tag = "not_red"
            }
        }

        wrapper.voteUpdatingState?.let { votingState ->
            if (votingState.state == UpdatingState.DONE) {
                getDownVotwProgress().setViewGone()
                getUpvoteProgress().setViewGone()
                getUpvoteText().setViewVisible()
                getUpvoteIv().setViewVisible()
                getDownVoteText().setViewVisible()
                getDownvoteIv().setViewVisible()
            } else {
                when {
                    votingState.votingStrength < 0 -> {//downvoting progress
                        getDownVotwProgress().setViewVisible()
                        getDownvoteIv().setViewInvisible()
                        getDownVoteText().setViewInvisible()


                        getUpvoteProgress().setViewGone()
                        getUpvoteIv().setViewVisible()
                        getUpvoteText().setViewVisible()
                    }
                    votingState.votingStrength > 0 -> {//upvoting progress
                        getUpvoteProgress().setViewVisible()
                        getUpvoteText().setViewInvisible()
                        getUpvoteIv().setViewInvisible()

                        getDownVotwProgress().setViewGone()
                        getDownvoteIv().setViewVisible()
                        getDownVoteText().setViewVisible()
                    }
                    else -> {//vote removing progress
                        val currentStatus = wrapper.voteStatus
                        if (currentStatus == GolosDiscussionItem.UserVoteType.VOTED) {
                            getUpvoteProgress().setViewVisible()
                            getDownVotwProgress().setViewGone()
                            getUpvoteIv().setViewInvisible()
                            getUpvoteText().setViewInvisible()
                            getDownVoteText().setViewVisible()
                            getDownvoteIv().setViewVisible()
                        } else if (currentStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                            getUpvoteProgress().setViewGone()
                            getDownVotwProgress().setViewVisible()
                            getUpvoteText().setViewVisible()
                            getUpvoteIv().setViewVisible()
                            getDownVoteText().setViewInvisible()
                            getDownvoteIv().setViewInvisible()
                        }
                    }
                }
            }
        }
    }

    open protected fun handleAvatarLoading(newState: StripeWrapper?,
                                           oldState: StripeWrapper?) {
        val wrapper = newState?.stripe

        val newAvatar = wrapper?.authorAccountInfo?.avatarPath


        if (newAvatar != null) {
            if (newAvatar != oldAvatar) {
                mGlide
                        .load(ImageUriResolver.resolveImageWithSize(
                                newAvatar,
                                wantedwidth = getAuthorAvatar().width))
                        .apply(RequestOptions().placeholder(StripeFullViewHolder.noAvatarDrawable))
                        .error(mGlide.load(StripeFullViewHolder.noAvatarDrawable))
                        .into(getAuthorAvatar())
                oldAvatar = newAvatar
            }
        } else getAuthorAvatar().setImageDrawable(StripeFullViewHolder.noAvatarDrawable)
    }

    protected abstract fun getMainImage(): ImageView
    protected abstract fun getTitleTv(): TextView
    protected abstract fun getCommentsTv(): TextView?
    protected abstract fun getShownUserNameTv(): TextView
    protected abstract fun getUserNickTv(): TextView
    protected abstract fun getReblogedByTv(): TextView
    protected abstract fun getBlogNameTv(): TextView
    protected abstract fun getDelimeterV(): View
    protected abstract fun getMoneyText(): TextView
    protected abstract fun getUpvoteIv(): ImageView
    protected abstract fun getUpvoteText(): TextView
    protected abstract fun getDownvoteIv(): ImageView
    protected abstract fun getDownVoteText(): TextView
    protected abstract fun getMainText(): TextView
    protected abstract fun getDateStampText(): TextView
    protected abstract fun getAuthorAvatar(): ImageView
    protected abstract fun getDownVotwProgress(): View
    protected abstract fun getUpvoteProgress(): View

    open fun handleImagePlacing(newState: StripeWrapper?,
                                imageView: ImageView) {
        val story = newState?.stripe?.story
        getMainImage().setImageDrawable(null)
        if (story == null) {
            return
        }

        if (!newState.isImagesShown) {
            imageView.setViewGone()
            imageView.setImageDrawable(null)
            return
        }
        if ((story.type == GolosDiscussionItem.ItemType.IMAGE_FIRST || showImageIfNotImageFirst())
                && story.tags.find {
                    val lowerCased = it.toLowerCase() // and there is nsfw tag
                    lowerCased == "nsfw" || lowerCased == "nswf"
                } != null) {

            if (newState.nswfStrategy.makeExceptionForUser.first &&
                    newState.nswfStrategy.makeExceptionForUser.second == story.author) {
            } else if (!newState.nswfStrategy.showNSFWImages) {
                if (story.images.size != 0) {
                    imageView.setViewVisible()
                    imageView.setImageResource(R.drawable.ic_nswf)

                    getMainText().setViewGone()
                } else {
                    imageView.setViewGone()
                    imageView.setImageDrawable(null)
                    getMainText().setViewVisible()
                }
                return
            }
        }

        if (story.type != GolosDiscussionItem.ItemType.IMAGE_FIRST) {
            if (showImageIfNotImageFirst()) {
                loadMainImage(story, imageView)
            } else {
                imageView.setViewGone()
                imageView.setImageDrawable(null)
            }
        } else {
            imageView.setViewVisible()
            loadMainImage(story, imageView)
        }
    }

    private fun loadMainImage(story: GolosDiscussionItem, imageView: ImageView) {
        val error = mGlide.load(getErrorDrawable())
        var nextImage: RequestBuilder<Drawable>? = null

        var size = imageView.width
        if (size <= 0) size = imageView.context.resources.displayMetrics.widthPixels
        if (size <= 0) size = 768
        if (story.images.size > 1) {
            nextImage = mGlide.load(ImageUriResolver.resolveImageWithSize(story.images[1], size)).error(error)
        }

        if (story.images.size > 0) {
            mGlide.load(ImageUriResolver.resolveImageWithSize(story.images[0],
                    wantedwidth = size))
                    .error(nextImage ?: error)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.placeholderOf(getErrorDrawable()))

                    .into(imageView)
        } else {

            val image = story.parts.find { it is ImageRow }

            image?.let {
                val src = (it as ImageRow).src
                mGlide.load(ImageUriResolver.resolveImageWithSize(src,
                        wantedwidth = size))
                        .error(nextImage ?: error)
                        .apply(RequestOptions.placeholderOf(getErrorDrawable()))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView)
            }
            if (image == null) {
                imageView.setImageResource(R.drawable.error)
            }
        }
    }

    protected abstract fun showImageIfNotImageFirst(): Boolean

    protected abstract fun getErrorDrawable(): Drawable


    protected open fun setUpTheme() {
        getCommentsTv()?.setTextColorCompat(R.color.text_color_white_black)
        getTitleTv().setTextColorCompat(R.color.text_color_white_black)
        getShownUserNameTv().setTextColorCompat(R.color.text_color_white_black)
        getUpvoteText().setTextColorCompat(R.color.text_color_white_black)
        getDownVoteText().setTextColorCompat(R.color.text_color_white_black)
        getReblogedByTv().setTextColorCompat(R.color.gray_82)
        getBlogNameTv().setTextColorCompat(R.color.gray_82)
        getDelimeterV().setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.delimeter_color_feed))
    }

    companion object {
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
    }
}

