package io.golos.golos.utils

import android.app.Activity
import android.graphics.RectF
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.*
import android.widget.TextView
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity

/**
 * Handles URL clicks on TextViews. Unlike the default implementation, this:
 *
 *
 *
 *  * Reliably applies a highlight color on links when they're touched.
 *  * Let's you handle single and long clicks on URLs
 *  * Correctly identifies focused URLs (Unlike the default implementation where a click is registered even if it's
 * made outside of the URL's bounds if there is no more text in that direction.)
 *
 */
class GolosMovementMethod protected constructor() : LinkMovementMethod() {

    private var onLinkClickListener: OnLinkClickListener? = object : OnLinkClickListener {
        override fun onClick(textView: TextView, url: String): Boolean {
            val matchResult = GolosLinkMatcher.match(url)
            if (matchResult is StoryLinkMatch) {
                StoryActivity.start(textView.context,
                        matchResult.author,
                        matchResult.blog,
                        matchResult.permlink,
                        FeedType.UNCLASSIFIED)
                return true
            }
            return false
        }
    }
    private var onLinkLongClickListener: OnLinkLongClickListener? = null
    private val touchedLineBounds = RectF()
    private var isUrlHighlighted: Boolean = false
    private var clickableSpanUnderTouchOnActionDown: ClickableSpan? = null
    private var activeTextViewHashcode: Int = 0
    private var ongoingLongPressTimer: LongPressTimer? = null
    private var wasLongPressRegistered: Boolean = false

    interface OnLinkClickListener {
        /**
         * @param textView The TextView on which a click was registered.
         * @param url      The clicked URL.
         * @return True if this click was handled. False to let Android handle the URL.
         */
        fun onClick(textView: TextView, url: String): Boolean
    }

    interface OnLinkLongClickListener {
        /**
         * @param textView The TextView on which a long-click was registered.
         * @param url      The long-clicked URL.
         * @return True if this long-click was handled. False to let Android handle the URL (as a short-click).
         */
        fun onLongClick(textView: TextView, url: String): Boolean
    }

    /**
     * Set a listener that will get called whenever any link is clicked on the TextView.
     */
    fun setOnLinkClickListener(clickListener: OnLinkClickListener): GolosMovementMethod {
        if (this === singleInstance) {
            throw UnsupportedOperationException("Setting a click listener on the instance returned by getInstance() is not supported to avoid memory " + "leaks. Please use newInstance() or any of the linkify() methods instead.")
        }

        this.onLinkClickListener = clickListener
        return this
    }

    /**
     * Set a listener that will get called whenever any link is clicked on the TextView.
     */
    fun setOnLinkLongClickListener(longClickListener: OnLinkLongClickListener): GolosMovementMethod {
        if (this === singleInstance) {
            throw UnsupportedOperationException("Setting a long-click listener on the instance returned by getInstance() is not supported to avoid " + "memory leaks. Please use newInstance() or any of the linkify() methods instead.")
        }

        this.onLinkLongClickListener = longClickListener
        return this
    }

    override fun onTouchEvent(textView: TextView, text: Spannable, event: MotionEvent): Boolean {
        if (activeTextViewHashcode != textView.hashCode()) {
            // Bug workaround: TextView stops calling onTouchEvent() once any URL is highlighted.
            // A hacky solution is to reset any "autoLink" property set in XML. But we also want
            // to do this once per TextView.
            activeTextViewHashcode = textView.hashCode()
            textView.autoLinkMask = 0
        }

        val clickableSpanUnderTouch = findClickableSpanUnderTouch(textView, text, event)
        val touchStartedOverALink = clickableSpanUnderTouchOnActionDown != null

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (clickableSpanUnderTouch != null) {
                    highlightUrl(textView, clickableSpanUnderTouch, text)
                }

                if (touchStartedOverALink && onLinkLongClickListener != null) {
                    val longClickListener = object : LongPressTimer.OnTimerReachedListener {
                        override fun onTimerReached() {
                            wasLongPressRegistered = true
                            textView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            removeUrlHighlightColor(textView)
                            dispatchUrlLongClick(textView, clickableSpanUnderTouch)
                        }
                    }
                    startTimerForRegisteringLongClick(textView, longClickListener)
                }

                clickableSpanUnderTouchOnActionDown = clickableSpanUnderTouch
                return touchStartedOverALink
            }

            MotionEvent.ACTION_UP -> {
                // Register a click only if the touch started and ended on the same URL.
                if (!wasLongPressRegistered && touchStartedOverALink && clickableSpanUnderTouch === clickableSpanUnderTouchOnActionDown) {
                    dispatchUrlClick(textView, clickableSpanUnderTouch ?: return false)
                }
                cleanupOnTouchUp(textView)

                // Consume this event even if we could not find any spans to avoid letting Android handle this event.
                // Android's TextView implementation has a bug where links get clicked even when there is no more text
                // next to the link and the touch lies outside its bounds in the same direction.
                return touchStartedOverALink
            }

            MotionEvent.ACTION_CANCEL -> {
                cleanupOnTouchUp(textView)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                // Stop listening for a long-press as soon as the user wanders off to unknown lands.
                if (clickableSpanUnderTouch !== clickableSpanUnderTouchOnActionDown) {
                    removeLongPressCallback(textView)
                }

                if (!wasLongPressRegistered) {
                    // Toggle highlight.
                    if (clickableSpanUnderTouch != null) {
                        highlightUrl(textView, clickableSpanUnderTouch, text)
                    } else {
                        removeUrlHighlightColor(textView)
                    }
                }

                return touchStartedOverALink
            }

            else -> return false
        }
    }

    private fun cleanupOnTouchUp(textView: TextView) {
        wasLongPressRegistered = false
        removeUrlHighlightColor(textView)
        removeLongPressCallback(textView)
    }

    /**
     * Determines the touched location inside the TextView's text and returns the ClickableSpan found under it (if any).
     *
     * @return The touched ClickableSpan or null.
     */
    protected fun findClickableSpanUnderTouch(textView: TextView, text: Spannable, event: MotionEvent): ClickableSpan? {
        // So we need to find the location in text where touch was made, regardless of whether the TextView
        // has scrollable text. That is, not the entire text is currently visible.
        var touchX = event.x.toInt()
        var touchY = event.y.toInt()

        // Ignore padding.
        touchX -= textView.totalPaddingLeft
        touchY -= textView.totalPaddingTop

        // Account for scrollable text.
        touchX += textView.scrollX
        touchY += textView.scrollY

        val layout = textView.layout
        val touchedLine = layout.getLineForVertical(touchY)
        val touchOffset = layout.getOffsetForHorizontal(touchedLine, touchX.toFloat())

        touchedLineBounds.left = layout.getLineLeft(touchedLine)
        touchedLineBounds.top = layout.getLineTop(touchedLine).toFloat()
        touchedLineBounds.right = layout.getLineWidth(touchedLine) + touchedLineBounds.left
        touchedLineBounds.bottom = layout.getLineBottom(touchedLine).toFloat()

        if (touchedLineBounds.contains(touchX.toFloat(), touchY.toFloat())) {
            // Find a ClickableSpan that lies under the touched area.
            val spans = text.getSpans(touchOffset, touchOffset, ClickableSpan::class.java)
            for (span in spans) {
                if (span is ClickableSpan) {
                    return span
                }
            }
            // No ClickableSpan found under the touched location.
            return null

        } else {
            // Touch lies outside the line's horizontal bounds where no spans should exist.
            return null
        }
    }

    /**
     * Adds a background color span at <var>clickableSpan</var>'s location.
     */
    protected fun highlightUrl(textView: TextView, clickableSpan: ClickableSpan, text: Spannable) {
        if (isUrlHighlighted) {
            return
        }
        isUrlHighlighted = true

        val spanStart = text.getSpanStart(clickableSpan)
        val spanEnd = text.getSpanEnd(clickableSpan)
        text.setSpan(BackgroundColorSpan(textView.highlightColor), spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        textView.text = text

        Selection.setSelection(text, spanStart, spanEnd)
    }

    /**
     * Removes the highlight color under the Url.
     */
    protected fun removeUrlHighlightColor(textView: TextView) {
        if (!isUrlHighlighted) {
            return
        }
        isUrlHighlighted = false

        val text = textView.text as Spannable

        val highlightSpans = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
        for (highlightSpan in highlightSpans) {
            text.removeSpan(highlightSpan)
        }

        textView.text = text

        Selection.removeSelection(text)
    }

    protected fun startTimerForRegisteringLongClick(textView: TextView, longClickListener: LongPressTimer.OnTimerReachedListener) {
        ongoingLongPressTimer = LongPressTimer()
        ongoingLongPressTimer!!.setOnTimerReachedListener(longClickListener)
        textView.postDelayed(ongoingLongPressTimer, ViewConfiguration.getLongPressTimeout().toLong())
    }

    /**
     * Remove the long-press detection timer.
     */
    protected fun removeLongPressCallback(textView: TextView) {
        if (ongoingLongPressTimer != null) {
            textView.removeCallbacks(ongoingLongPressTimer)
            ongoingLongPressTimer = null
        }
    }

    protected fun dispatchUrlClick(textView: TextView, clickableSpan: ClickableSpan) {
        val clickableSpanWithText = ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkClickListener != null && onLinkClickListener!!.onClick(textView, clickableSpanWithText.text())

        if (!handled) {
            // Let Android handle this click.
            clickableSpanWithText.span().onClick(textView)
        }
    }

    protected fun dispatchUrlLongClick(textView: TextView, clickableSpan: ClickableSpan?) {
        val clickableSpanWithText = ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkLongClickListener != null && onLinkLongClickListener!!.onLongClick(textView, clickableSpanWithText.text())

        if (!handled) {
            // Let Android handle this long click as a short-click.
            clickableSpanWithText.span().onClick(textView)
        }
    }

    protected class LongPressTimer : Runnable {
        private var onTimerReachedListener: OnTimerReachedListener? = null

        interface OnTimerReachedListener {
            fun onTimerReached()
        }

        override fun run() {
            onTimerReachedListener!!.onTimerReached()
        }

        fun setOnTimerReachedListener(listener: OnTimerReachedListener) {
            onTimerReachedListener = listener
        }
    }

    /**
     * A wrapper to support all [ClickableSpan]s that may or may not provide URLs.
     */
    protected class ClickableSpanWithText private constructor(private val span: ClickableSpan, private val text: String) {

        internal fun span(): ClickableSpan {
            return span
        }

        internal fun text(): String {
            return text
        }

        companion object {

            fun ofSpan(textView: TextView, span: ClickableSpan?): ClickableSpanWithText {
                val s = textView.text as Spanned
                val text: String
                if (span is URLSpan) {
                    text = span.url
                } else {
                    val start = s.getSpanStart(span)
                    val end = s.getSpanEnd(span)
                    text = s.subSequence(start, end).toString()
                }
                return ClickableSpanWithText(span!!, text)
            }
        }
    }

    companion object {

        private var singleInstance: GolosMovementMethod? = null
        private val LINKIFY_NONE = -2

        /**
         * Return a new instance of BetterLinkMovementMethod.
         */
        fun newInstance(): GolosMovementMethod {
            return GolosMovementMethod()
        }

        /**
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @param textViews   The TextViews on which a [GolosMovementMethod] should be registered.
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, vararg textViews: TextView): GolosMovementMethod {
            val movementMethod = newInstance()
            for (textView in textViews) {
                addLinks(linkifyMask, movementMethod, textView)
            }
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @param textViews The TextViews on which a [GolosMovementMethod] should be registered.
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkifyHtml(vararg textViews: TextView): GolosMovementMethod {
            return linkify(LINKIFY_NONE, *textViews)
        }

        /**
         * Recursively register a [GolosMovementMethod] on every TextView inside a layout.
         *
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, viewGroup: ViewGroup): GolosMovementMethod {
            val movementMethod = newInstance()
            rAddLinks(linkifyMask, viewGroup, movementMethod)
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkifyHtml(viewGroup: ViewGroup): GolosMovementMethod {
            return linkify(LINKIFY_NONE, viewGroup)
        }

        /**
         * Recursively register a [GolosMovementMethod] on every TextView inside a layout.
         *
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, activity: Activity): GolosMovementMethod {
            // Find the layout passed to setContentView().
            val activityLayout = (activity.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup).getChildAt(0) as ViewGroup

            val movementMethod = newInstance()
            rAddLinks(linkifyMask, activityLayout, movementMethod)
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @return The registered [GolosMovementMethod] on the TextViews.
         */
        fun linkifyHtml(activity: Activity): GolosMovementMethod {
            return linkify(LINKIFY_NONE, activity)
        }

        /**
         * Get a static instance of BetterLinkMovementMethod. Do note that registering a click listener on the returned
         * instance is not supported because it will potentially be shared on multiple TextViews.
         */
        val instance: GolosMovementMethod
            get() {
                if (singleInstance == null) {
                    singleInstance = GolosMovementMethod()
                }
                return singleInstance!!
            }

        // ======== PUBLIC APIs END ======== //

        private fun rAddLinks(linkifyMask: Int, viewGroup: ViewGroup, movementMethod: GolosMovementMethod) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)

                if (child is ViewGroup) {
                    // Recursively find child TextViews.
                    rAddLinks(linkifyMask, child, movementMethod)

                } else if (child is TextView) {
                    addLinks(linkifyMask, movementMethod, child)
                }
            }
        }

        private fun addLinks(linkifyMask: Int, movementMethod: GolosMovementMethod, textView: TextView) {
            textView.movementMethod = movementMethod
            if (linkifyMask != LINKIFY_NONE) {
                Linkify.addLinks(textView, linkifyMask)
            }
        }
    }
}