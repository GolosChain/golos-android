package io.golos.golos.utils

import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.Html
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.QuoteSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.snackbar.Snackbar
import eu.bittrade.libs.golosj.base.models.Account
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.base.models.operations.CommentOperation
import eu.bittrade.libs.golosj.communication.CommunicationHandler
import eu.bittrade.libs.golosj.util.ImmutablePair
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.editor.getDimen
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.KnifeQuoteSpan
import io.golos.golos.screens.editor.knife.NumberedMarginSpan
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWrapper
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */

const val siteUrl = BuildConfig.BASE_URL

object Counter {
    val counter = AtomicInteger(-1)
}

public fun Any.nextInt() = Counter.counter.incrementAndGet()

fun Cursor.getString(columnName: String): String? {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return null
    return this.getString(this.getColumnIndex(columnName))
}

inline fun traceCaller() {
    if (BuildConfig.DEBUG) {
        Timber.e(Thread.currentThread().stackTrace[4].toString())
    }
}

inline fun <reified K, reified V> Map<K, V>.toHashMap(): HashMap<K, V> {
    return if (this is HashMap<K, V>) return this
    else HashMap(this)
}

fun Cursor.getLong(columnName: String): Long {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return 0L
    return this.getLong(this.getColumnIndex(columnName))
}

fun GolosDiscussionItem.isStory() = this.parentAuthor.isEmpty()

fun GolosDiscussionItem.isComment() = this.parentAuthor.isNotEmpty()

fun createStoryWrapper(discussionItem: GolosDiscussionItem,
                       voteStatuses: List<GolosDiscussionItemVotingState>,
                       golosUsersAccounts: Map<String, GolosUserAccountInfo>,
                       repostedBlogEntries: Map<String, GolosBlogEntry>,
                       repostStates: Map<String, RepostingState>,
                       currentUser: ApplicationUser?,
                       exchangeValues: ExchangeValues,
                       isThereNeedToHtmlize: Boolean,
                       htmlizer: Htmlizer?): StoryWrapper {
    if (isThereNeedToHtmlize && htmlizer == null) throw java.lang.IllegalArgumentException("if isThereNeedToHtmlize set to true htmlizer cannot be null")


    val out = StoryWrapper(discussionItem,
            voteStatuses.find { it.storyId == discussionItem.id },
            if (currentUser == null || !currentUser.isLogged) GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT else discussionItem.isUserVotedOnThis(currentUser.name),
            if (currentUser == null || !currentUser.isLogged) false else repostedBlogEntries.containsKey(discussionItem.permlink),
            repostStates[discussionItem.permlink]?.updatingState ?: UpdatingState.DONE,
            golosUsersAccounts[discussionItem.author],
            exchangeValues,
            currentUser != null && discussionItem.author == currentUser.name,
            if (isThereNeedToHtmlize) htmlizer!!.toHtml(discussionItem.cleanedFromImages) else null)

    return out
}


fun Cursor.getBool(columnName: String): Boolean {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return false
    return this.getInt(this.getColumnIndex(columnName)) > 0
}

fun Cursor.getInt(columnName: String): Int {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return 0
    return this.getInt(this.getColumnIndex(columnName))
}

fun Activity.restart() {
    this.recreate()
}

fun <T : View> ViewGroup.inflate(@LayoutRes layoutResId: Int): T {
    return LayoutInflater.from(this.context).inflate(layoutResId, this, false) as T
}

fun Cursor.getDouble(columnName: String): Double {
    return this.getDouble(this.getColumnIndex(columnName))
}

fun String.asIntentToShowUrl(): Intent {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(this);
    return i
}

public fun allGolosUrls() = BuildConfig.ALTERNATE_URLS + BuildConfig.BASE_URL

inline fun <reified T : Any> createIntent(vararg pairs: Pair<String, T>): Intent {
    val out = Intent()
    pairs.forEach {
        out.putExtra(it.first, when (it.second) {
            is Boolean -> it
            is Byte -> it
            is Char -> it
            is Short -> it
            is Int -> it
            is Long -> it
            is Float -> it
            is Double -> it
            is String -> it
            is CharSequence -> it
            is Parcelable -> it
            is Serializable -> it
            is BooleanArray -> it
            is ByteArray -> it
            is CharArray -> it
            is ShortArray -> it
            is IntArray -> it
            is LongArray -> it
            is FloatArray -> it
            is DoubleArray -> it
            is Bundle -> it
            is Array<*> -> it
            else -> throw IllegalArgumentException("cannot pu argiment of type ${it.second::class.java}")
        })
    }
    return out
}

fun String.asIntentToShareString(): Intent {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, this)
    sendIntent.type = "text/plain"
    return sendIntent
}

fun TextView.setTextColorCompat(@ColorRes colorId: Int) {
    this.setTextColor(ContextCompat.getColor(this.context, colorId))
}

fun Context.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(this, coloId)
}


fun Fragment.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(activity!!, coloId)
}

fun View.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(context!!, coloId)
}

fun fromString(string: String): FeedType? {
    return when (string.toLowerCase()) {
        "trending" -> FeedType.POPULAR
        "created" -> FeedType.NEW
        "blog" -> FeedType.BLOG
        "hot" -> FeedType.ACTUAL
        "promoted" -> FeedType.PROMO
        "comments" -> FeedType.COMMENTS
        else -> null
    }
}

fun Fragment.showProgressDialog(): ProgressDialog {
    val dialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
    dialog.isIndeterminate = true
    dialog.setCancelable(false)
    dialog.show()
    val progress = dialog.findViewById<View>(android.R.id.progress) as ProgressBar
    progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context!!, R.color.colorAccent), PorterDuff.Mode.MULTIPLY)
    dialog.setTitle(R.string.loading)
    return dialog
}

fun Activity.showProgressDialog(): ProgressDialog {
    val dialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
    dialog.isIndeterminate = true
    dialog.setCancelable(false)
    dialog.show()
    val progress = dialog.findViewById<View>(android.R.id.progress) as ProgressBar
    progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), PorterDuff.Mode.MULTIPLY)
    dialog.setTitle(R.string.loading)
    return dialog
}

fun ViewGroup.setFullAnimationToViewGroup() {
    val layoutTransition = LayoutTransition()
    layoutTransition.enableTransitionType(LayoutTransition.APPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    this.layoutTransition = layoutTransition
}

fun Context.hideKeyboard(currentFocus: View) {
    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
}

fun SearchView.setTextColorHint(@ColorRes coloId: Int) {
    try {
        (this.findViewById<EditText>(androidx.appcompat.R.id.search_src_text) as EditText)
                .setHintTextColor(this.getColorCompat(R.color.text_color_white_black))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Long.hoursElapsedFromTimeStamp(): Int {
    val currentTime = System.currentTimeMillis() - TimeZone.getDefault().getOffset(System.currentTimeMillis())
    val dif = currentTime - this
    val hour = 1000 * 60 * 60
    val hoursAgo = dif / hour
    return hoursAgo.toInt()
}


fun createTimeLabel(fromTimeStamp: Long, context: Context): String {
    val mSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    return fromTimeStamp.hoursElapsedFromTimeStamp().let { elapsedHoursFromPostCreation ->
        when {
            elapsedHoursFromPostCreation == 0 -> context.resources.getString(R.string.less_then_hour_ago)
            elapsedHoursFromPostCreation < 24 -> "$elapsedHoursFromPostCreation ${context.resources.getQuantityString(R.plurals.hours, elapsedHoursFromPostCreation)} ${context.resources.getString(R.string.ago)}"
            else -> {
                val daysAgo = Math.round(elapsedHoursFromPostCreation.toDouble() / 24)

                if (daysAgo <= 7)
                    "$daysAgo ${context.resources.getQuantityString(R.plurals.days, daysAgo.toInt())} ${context.resources.getString(R.string.ago)}"
                else {
                    val timeStamp = fromTimeStamp + TimeZone.getDefault().getOffset(fromTimeStamp)
                    Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = timeStamp }.let {
                        mSdf.format(it.time)
                    }
                }
            }
        }
    }
}

val Account.avatarPath: String?
    get() {
        var avatarPath: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                val node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    avatarPath = node.get("profile")?.get("profile_image")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return avatarPath
    }


val Account.shownName: String?
    get() {
        var shownName: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                val node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    shownName = node.get("profile")?.get("name")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return shownName
    }


val Account.cover: String?
    get() {
        var avatarPath: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                val node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    avatarPath = node.get("profile")?.get("cover_image")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return avatarPath
    }

public fun Context.getLayoutInflater(): LayoutInflater = LayoutInflater.from(this)

public fun View.getLayoutInflater(): LayoutInflater = context.getLayoutInflater()

val Account.moto: String?
    get() {
        var moto: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                val node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    moto = node.get("profile")?.get("about")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return moto
    }
val Account.location: String?
    get() {
        var moto: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    moto = node.get("profile")?.get("location")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return moto
    }
val Account.webSite: String?
    get() {
        var moto: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    moto = node.get("profile")?.get("website")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return moto
    }

fun String.toHtml(): Spanned {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)

    } else {
        return Html.fromHtml(this)
    }
}

fun <T : Any?> Context.createGolosSpan(type: Class<*>): T {
    val out = when (type) {
        KnifeBulletSpan::class.java -> KnifeBulletSpan(getColorCompat(R.color.blue_light),
                getDimen(R.dimen.quint).toInt(),
                getDimen(R.dimen.quater).toInt()) as T
        NumberedMarginSpan::class.java -> NumberedMarginSpan(12, getDimen(R.dimen.margin_material_half).toInt(),
                1) as T
        KnifeQuoteSpan::class.java -> KnifeQuoteSpan(getColorCompat(R.color.blue_light),
                getDimen(R.dimen.quint).toInt(),
                getDimen(R.dimen.quater).toInt()) as T
        AbsoluteSizeSpan::class.java -> AbsoluteSizeSpan(getDimen(R.dimen.font_medium).toInt()) as T
        QuoteSpan::class.java -> KnifeBulletSpan(getColorCompat(R.color.blue_light),
                getDimen(R.dimen.quint).toInt(),
                getDimen(R.dimen.quater).toInt()) as T
        else -> Any() as T
    }

    return out
}

fun SwipeRefreshLayout.setRefreshingS(isRefreshing: Boolean) {
    if (isRefreshing && !this.isRefreshing) this.isRefreshing = true
    else if (!isRefreshing && this.isRefreshing) this.isRefreshing = false
}


fun View.showSnackbar(message: Int) {
    if (message == 0) return
    Snackbar.make(this,
            Html.fromHtml("<font color=\"#ffffff\">${resources.getString(message)}</font>"),
            Snackbar.LENGTH_SHORT).show()
}

fun Any?.isNull(): Boolean = this == null

fun View.showSnackbar(message: String) {
    Snackbar.make(this,
            Html.fromHtml("<font color=\"#ffffff\">${message}</font>"),
            Snackbar.LENGTH_SHORT).show()
}

fun View.setViewGone() {
    if (this.visibility != View.GONE) this.visibility = View.GONE
}

fun View.setViewVisible() {
    if (this.visibility != View.VISIBLE) {
        visibility = View.VISIBLE
        isClickable = true
    }
}

fun View.setViewInvisible() {
    if (this.visibility != View.INVISIBLE) {
        visibility = View.INVISIBLE
        isClickable = false
    }

}


fun changeRepostState(inputWrapper: StoryWrapper,
                      userBlogEntries: Map<String, GolosBlogEntry>,
                      userRepostingStates: Map<String, RepostingState>): StoryWrapper {
    val reposts = userBlogEntries
    val repostStates = userRepostingStates

    val isPostReposted = reposts.containsKey(inputWrapper.story.permlink)
    val repsotState = repostStates[inputWrapper.story.permlink]?.updatingState
            ?: UpdatingState.DONE
    return if (inputWrapper.isPostReposted == isPostReposted && inputWrapper.repostStatus == repsotState) inputWrapper
    else inputWrapper.copy(isPostReposted = reposts.containsKey(inputWrapper.story.permlink),
            repostStatus = repostStates[inputWrapper.story.permlink]?.updatingState
                    ?: UpdatingState.DONE)

}

//compares content and position in collection
inline fun <reified T> List<T>.compareContents(other: List<T>): Boolean {

    if (size != other.size) return false
    forEachIndexed { index, t ->
        if (t != other[index]) return false
    }

    return true
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(
            Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInputFromInputMethod(windowToken, 0)
}

fun Context.getVectorDrawable(@DrawableRes resId: Int): Drawable {
    return AppCompatResources.getDrawable(this, resId)!!
}

fun View.getVectorDrawable(@DrawableRes resId: Int): Drawable {
    return AppCompatResources.getDrawable(context, resId)!!
}

fun View.getVectorDrawableWithIntrinisticSize(@DrawableRes resId: Int): Drawable {
    val d = AppCompatResources.getDrawable(context, resId)!!
    d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
    return d;
}

fun Context.getVectorAsBitmap(@DrawableRes resId: Int): Bitmap {

    var drawable = AppCompatResources.getDrawable(this, resId)!!
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = (DrawableCompat.wrap(drawable)).mutate()
    }

    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap

}

fun Drawable.getVectorAsBitmap(): Bitmap {

    var drawable = this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = (DrawableCompat.wrap(this)).mutate()
    }
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap

}

fun CommentOperation.getTags(): List<String> {
    val out = ArrayList<String>()
    try {
        if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
            var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
            node?.let {
                val tags = node.get("tags")?.asIterable()
                tags?.forEach {
                    out.add(it.asText())
                }
            }
        }

    } catch (e: IOException) {
        println("error parsing metadata " + jsonMetadata)
        e.printStackTrace()
    }
    return out

}

fun <E> List<out E>.toArrayList(): ArrayList<E> {
    return ArrayList(this)
}

fun File.sizeInKb(): Long {
    return length() / 1024
}


public inline fun <E> List<out E>?.isNullOrEmpty(): Boolean = this == null || this.size == 0


public inline fun <reified V> bundleOf(vararg pairs: Pair<String, V>): Bundle {
    val b = Bundle()
    pairs.forEach {
        val second = it.second
        when (second) {
            is Short -> b.putShort(it.first, second)
            is Int -> b.putInt(it.first, second)
            is Long -> b.putLong(it.first, second)
            is String -> b.putString(it.first, second)
            is Serializable -> b.putSerializable(it.first, second)
            is ArrayList<*> -> {
                if (second.isNotEmpty() && second[0] !is String) {
                    throw IllegalArgumentException("only arraylist of strings supported")
                } else {
                    b.putStringArrayList(it.first, second as java.util.ArrayList<String>)
                }
            }
            else -> throw IllegalArgumentException("unsupported")
        }
    }
    return b
}

public fun TextView.setVectorDrawableStart(id: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(this.getVectorDrawable(id), null, null, null)
}

public fun TextView.setVectorDrawableEnd(id: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(null, null, this.getVectorDrawable(id), null)
}

fun isOnMainThread(): Boolean {
    if (BuildConfig.DEBUG) return true
    return (Looper.getMainLooper() == Looper.myLooper())
}

public fun Context.getQuantityString(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any?) = resources.getQuantityString(id, quantity, *formatArgs)

fun calculateShownReward(wrapper: StoryWrapper,
                         chosenCurrency: UserSettingsRepository.GolosCurrency = Repository.get.userSettingsRepository.getCurrency().value
                                 ?: UserSettingsRepository.GolosCurrency.USD,
                         bountyDisplay: UserSettingsRepository.GolosBountyDisplay = Repository.get.userSettingsRepository.getBountDisplay().value
                                 ?: UserSettingsRepository.GolosBountyDisplay.THREE_PLACES,
                         ctx: Context): String {
    val gbgCost = wrapper.story.gbgAmount
    val resources = ctx.resources
    val exchangeValues = wrapper.exchangeValues
    if (exchangeValues == ExchangeValues.nullValues) {
        return resources.getString(R.string.gbg_format, bountyDisplay.formatNumber(gbgCost))
    } else {
        return when (chosenCurrency) {
            UserSettingsRepository.GolosCurrency.RUB -> resources.getString(R.string.rubles_format, bountyDisplay.formatNumber(gbgCost
                    * exchangeValues.rublesPerGbg))
            UserSettingsRepository.GolosCurrency.GBG -> resources.getString(R.string.gbg_format, bountyDisplay.formatNumber(gbgCost))
            else -> resources.getString(R.string.dollars_format, bountyDisplay.formatNumber(gbgCost
                    * exchangeValues.dollarsPerGbg))
        }
    }
}

fun ValueAnimator.setStartDelayB(delay: Long): ValueAnimator {
    startDelay = delay
    return this
}

fun ValueAnimator.setInterpolatorB(interpolator: TimeInterpolator): ValueAnimator {
    this.interpolator = interpolator
    return this
}

fun Fragment.getDimension(@DimenRes dimen: Int): Int {
    return context?.resources?.getDimension(dimen)?.toInt() ?: 0
}

fun View.getDimension(@DimenRes dimen: Int): Int {
    return context?.resources?.getDimension(dimen)?.toInt() ?: 0
}

fun Context.getDimen(@DimenRes resid: Int): Float {
    return resources.getDimension(resid)
}

fun View.getDimen(@DimenRes resid: Int): Float {
    return resources.getDimension(resid)
}

public fun ViewGroup.iterator(): Iterator<View> {
    return object : Iterator<View> {
        private var currentPosition = 0

        override fun hasNext(): Boolean {
            return currentPosition < childCount
        }

        override fun next(): View {
            val v = getChildAt(currentPosition)
            currentPosition++
            return v
        }
    }
}

fun AccountName?.isNullOrEmpty(): Boolean {
    return this?.name.isNullOrEmpty()
}

public operator fun ViewGroup.get(position: Int) = if (position < childCount) getChildAt(position) else null
/**
 * Created by yuri on 06.11.17.
 */
val mapper by lazy {
    val mapper = jacksonObjectMapper()
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    mapper
}

fun StringBuilder.removeString(str: String): StringBuilder {
    val index = indexOf(str)
    if (index > -1) {
        delete(index, index + str.length)
        removeString(str)
    }
    return this
}

public fun StringBuilder.replaceSb(regex: Regex, transform: (kotlin.text.MatchResult) -> CharSequence): StringBuilder {
    val str = replace(regex, transform)
    this.replace(0, length, str)
    return this
}

public fun StringBuilder.replaceSb(regex: Regex, to: String): StringBuilder {
    var match: MatchResult? = regex.find(this, 0) ?: return this

    var lastStart = 0
    val length = length
    do {
        val foundMatch = match!!
        append(this, lastStart, foundMatch.range.start)
        append(to)
        lastStart = foundMatch.range.endInclusive + 1
        match = foundMatch.next()
    } while (lastStart < length && match != null)

    if (lastStart < length) {
        append(this, lastStart, length)
    }


    return this
}

public fun StringBuilder.replaceSb(what: String, to: String): StringBuilder {
    var index = indexOf(what)
    while (index != -1) {
        replace(index, index + what.length, to)
        index += to.length // Move to the end of the replacement
        index = indexOf(what, index)
    }
    return this
}

public inline infix fun <A, B> A.to(that: B): ImmutablePair<A, B> = ImmutablePair(this, that)

public inline infix fun <A, B> A.toN(that: B): Pair<A, B> = Pair(this, that)


public fun <K, V> mutableMapOf(pairs: List<Pair<K, V>>): MutableMap<K, V> = LinkedHashMap<K, V>(pairs.size).apply { putAll(pairs) }