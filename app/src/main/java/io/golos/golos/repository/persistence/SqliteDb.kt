package io.golos.golos.repository.persistence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import eu.bittrade.libs.golosj.base.models.VoteLight
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.*
import java.util.*

/**
 * Created by yuri on 06.11.17.
 */
private val dbVersion = 11

class SqliteDb(ctx: Context) : SQLiteOpenHelper(ctx, "mydb.db", null, dbVersion) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(TagsTable.createTableString)
        db?.execSQL(UserFilterTable.createTableString)
        db?.execSQL(VotesTable.createTableString)
        db?.execSQL(StoriesRequestsTable.createTableString)
        db?.execSQL(DiscussionItemsTable.createTableString)
        db?.execSQL(SubscribersTable.createTableString)
        db?.execSQL(SubscriptionsTable.createTableString)
        db?.execSQL(UsersTable.createTableString)
        db?.execSQL(BlogEntriesTable.createTableString)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        db?.execSQL("drop table if exists ${TagsTable.databaseName}")
        db?.execSQL("drop table if exists ${UserFilterTable.databaseName}")
        db?.execSQL("drop table if exists ${VotesTable.databaseName}")
        db?.execSQL("drop table if exists ${StoriesRequestsTable.databaseName}")
        db?.execSQL("drop table if exists ${DiscussionItemsTable.databaseName}")
        db?.execSQL("drop table if exists ${SubscribersTable.databaseName}")
        db?.execSQL("drop table if exists ${SubscriptionsTable.databaseName}")
        db?.execSQL("drop table if exists ${UsersTable.databaseName}")
        db?.execSQL("drop table if exists ${BlogEntriesTable.databaseName}")

        db?.execSQL(TagsTable.createTableString)
        db?.execSQL(UserFilterTable.createTableString)
        db?.execSQL(VotesTable.createTableString)
        db?.execSQL(StoriesRequestsTable.createTableString)
        db?.execSQL(DiscussionItemsTable.createTableString)
        db?.execSQL(SubscribersTable.createTableString)
        db?.execSQL(SubscriptionsTable.createTableString)
        db?.execSQL(UsersTable.createTableString)
        db?.execSQL(BlogEntriesTable.createTableString)

    }


    fun saveTags(list: List<Tag>) {
        TagsTable.save(list, writableDatabase)
    }


    fun getTags(): List<Tag> {
        return TagsTable.get(writableDatabase)
    }

    fun saveTagsForFilter(tags: List<Tag>, filter: String) {
        UserFilterTable.save(tags, filter, writableDatabase)
    }

    fun getTagsForFilter(filter: String): List<Tag> {
        return UserFilterTable.get(filter, TagsTable, writableDatabase)
    }


    fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {
        val discussionItem = stories
                .map {
                    var items = it.value.items
                    if (items.size > 10) items = items.slice(0..10).toArrayList()
                    items
                }
                .flatMap { it }
                .mapNotNull { it.rootStory }
        DiscussionItemsTable.save(discussionItem, VotesTable, writableDatabase)
        val storiesIds = stories
                .map {
                    FilteredStoriesIds(it.key, it.value.items
                            .map { it.rootStory.id })
                }
        StoriesRequestsTable.save(storiesIds, writableDatabase)
    }


    fun getStories(): Map<StoryRequest, StoriesFeed> {

        val storiesIds = StoriesRequestsTable.get(writableDatabase)

        val votes = VotesTable.getAll(writableDatabase)

        val stories = DiscussionItemsTable.getAll(writableDatabase)


        return storiesIds
                .groupBy { it }
                .mapValues {
                    val filteredStories = it.key.storyIds.mapNotNull { stories[it] }
                    StoriesFeed(filteredStories
                            .map { StoryWithComments(it, arrayListOf()) }.toArrayList(),
                            it.key.request.feedType,
                            it.key.request.filter)
                }
                .onEach {
                    it.value.items.forEach {
                        it.rootStory.activeVotes.addAll(votes[it.rootStory.id] ?: arrayListOf())
                    }
                }

                .mapKeys { it.key.request }
                .mapValues {
                    it.value
                }
    }

    fun deleteAllStories() {
        VotesTable.deleteAll(writableDatabase)
        StoriesRequestsTable.deleteAll(writableDatabase)
        DiscussionItemsTable.deleteAll(writableDatabase)
    }


    fun saveGolosUsersAccountInfo(list: List<GolosUserAccountInfo>) {
        UsersTable.saveGolosUsersAccountInfo(writableDatabase, list)
    }

    fun saveGolosUsersSubscribers(map: Map<String, List<String>>) {
        SubscribersTable.saveGolosUsersSubscribers(writableDatabase, map)
    }

    fun getGolosUsersSubscribers(): Map<String, List<String>> {
        return SubscribersTable.getGolosUsersSubscribers(writableDatabase)

    }

    fun saveGolosUsersSubscriptions(map: Map<String, List<String>>) {
        SubscriptionsTable.saveGolosUsersSubscriptions(writableDatabase, map)
    }

    fun getGolosUsersAccountInfo(): List<GolosUserAccountInfo> {
        return UsersTable.getGolosUsersAccountInfo(writableDatabase)

    }


    fun getGolosUsersSubscriptions(): Map<String, List<String>> {
        return SubscriptionsTable.getGolosUsersSubscriptions(writableDatabase)
    }

    fun saveBlogEntries(entries: List<GolosBlogEntry>) {
        BlogEntriesTable.saveEntries(writableDatabase, entries)
    }

    fun getBlogEntries(): List<GolosBlogEntry> {
        return BlogEntriesTable.getEntries(writableDatabase)
    }

    fun deleteBlogEntries() {
        BlogEntriesTable.removeAllEntries(writableDatabase)
    }

    private object SubscribersTable {
        const val databaseName = "subscribers_table"
        private const val userName = "user_name"
        private const val subscribers = "subscribers"
        const val createTableString = "create table if not exists $databaseName ( $userName text primary key, $subscribers text )"

        fun saveGolosUsersSubscribers(writableDatabase: SQLiteDatabase?,
                                      map: Map<String, List<String>>) {
            writableDatabase ?: return
            writableDatabase.beginTransaction()
            val values = ContentValues()
            map.forEach {
                values.put(userName, it.key)
                values.put(subscribers, mapper.writeValueAsString(it.value))
                writableDatabase.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }

        fun getGolosUsersSubscribers(writableDatabase: SQLiteDatabase?): Map<String, List<String>> {
            val out = HashMap<String, List<String>>()
            writableDatabase ?: return out

            val c = writableDatabase.rawQuery("select * from $databaseName", emptyArray())
            if (c.moveToFirst()) {
                val collectionType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
                while (!c.isAfterLast) {
                    val userName = c.getString(userName).orEmpty()
                    val subscribersString = c.getString(subscribers)
                    val subscribers = mapper.readValue<List<String>>(subscribersString, collectionType)
                    out[userName] = subscribers
                    c.moveToNext()
                }
            }
            c.close()
            return out
        }
    }

    private object BlogEntriesTable {
        const val databaseName = "blog_entries_table"
        private const val userName = "user_name"
        private const val blogOwner = "blog_owner"
        private const val entryid = "entry_id"
        private const val permlink = "permlink"
        const val createTableString = "create table if not exists $databaseName ( $entryid integer primary key, $userName text," +
                "$blogOwner text, $permlink text)"

        fun saveEntries(writableDatabase: SQLiteDatabase, entries: List<GolosBlogEntry>) {
            writableDatabase.beginTransaction()
            val cv = ContentValues()
            entries.forEach {
                cv.put(userName, it.author)
                cv.put(blogOwner, it.blogOwner)
                cv.put(entryid, it.entryId)
                cv.put(permlink, it.permlink)
                writableDatabase.insertWithOnConflict(databaseName, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }

        fun removeAllEntries(writableDatabase: SQLiteDatabase) {
            writableDatabase.delete(databaseName, null, null)
        }

        fun getEntries(writableDatabase: SQLiteDatabase): List<GolosBlogEntry> {
            val out = arrayListOf<GolosBlogEntry>()
            val c = writableDatabase.rawQuery("select * from $databaseName ORDER BY $entryid desc", emptyArray())
            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    out.add(
                            GolosBlogEntry(c.getString(userName) ?: "",
                                    c.getString(blogOwner) ?: "",
                                    c.getInt(entryid),
                                    c.getString(permlink) ?: ""))
                    c.moveToNext()
                }

            }
            c.close()

            return out
        }
    }

    private object SubscriptionsTable {
        const val databaseName = "subscriptions_table"
        private const val userName = "user_name"
        private const val subscriptions = "subscriptions"
        const val createTableString = "create table if not exists $databaseName ( $userName text primary key, $subscriptions text )"

        fun saveGolosUsersSubscriptions(writableDatabase: SQLiteDatabase?,
                                        map: Map<String, List<String>>) {
            writableDatabase ?: return
            writableDatabase.beginTransaction()
            val values = ContentValues()
            map.forEach {
                values.put(userName, it.key)
                values.put(subscriptions, mapper.writeValueAsString(it.value))
                writableDatabase.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }

        fun getGolosUsersSubscriptions(writableDatabase: SQLiteDatabase?): Map<String, List<String>> {
            val out = HashMap<String, List<String>>()
            writableDatabase ?: return out

            val c = writableDatabase.rawQuery("select * from $databaseName", emptyArray())
            if (c.moveToFirst()) {
                val collectionType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
                while (!c.isAfterLast) {
                    val userName = c.getString(userName).orEmpty()
                    val subscribersString = c.getString(subscriptions)
                    val subscribers = mapper.readValue<List<String>>(subscribersString, collectionType)
                    out[userName] = subscribers
                    c.moveToNext()
                }
            }
            c.close()
            return out
        }
    }

    private object UsersTable {
        const val databaseName = "users_table"
        const val userName = "user_name"
        const val showName = "shown_name"
        const val avatarPath = "avatar_path"
        const val userMotto = "user_motto"
        const val postsCount = "posts_count"
        const val accountWorth = "account_worth"
        const val gbgAmount = "gbg_amount"
        const val golosAmount = "golos_amount"
        const val golosPower = "golos_power"
        const val safeGbg = "safe_gbg"
        const val safeGolos = "safe_golos"
        const val subscribersCount = "subscribers_count"
        const val subscriptionsCount = "subscriptions_count"
        const val postingPublicKey = "posting_key"
        const val activePublicKey = "active_key"
        const val votingPower = "voting_power"
        const val location = "location"
        const val website = "website"
        const val registrationDate = "registration_date"
        const val userCover = "user_cover"
        const val lastTimeInfoUpdatedAt = "last_time_updated"

        const val createTableString = "create table if not exists $databaseName ( $userName text primary key," +
                "$avatarPath text, $userMotto text, $postsCount integer, $accountWorth real, $gbgAmount real," +
                "$golosAmount real, $golosPower real, $safeGbg real, $safeGolos real, $subscribersCount integer," +
                " $subscriptionsCount integer, $postingPublicKey text, $activePublicKey text, $votingPower integer," +
                "$location text, $website text, $registrationDate integer, $userCover text, $lastTimeInfoUpdatedAt text, $showName text) "


        fun saveGolosUsersAccountInfo(writableDatabase: SQLiteDatabase?,
                                      list: List<GolosUserAccountInfo>) {
            writableDatabase ?: return
            val values = ContentValues()
            writableDatabase.beginTransaction()
            list.forEach {
                values.put(userName, it.userName)
                values.put(avatarPath, it.avatarPath)
                values.put(userMotto, it.userMotto)
                values.put(postsCount, it.postsCount)
                values.put(accountWorth, it.accountWorth)
                values.put(gbgAmount, it.gbgAmount)
                values.put(golosAmount, it.golosAmount)
                values.put(golosPower, it.golosPower)
                values.put(safeGbg, it.safeGbg)
                values.put(safeGolos, it.safeGolos)
                values.put(subscribersCount, it.subscribersCount)
                values.put(subscriptionsCount, it.subscriptionsCount)
                values.put(postingPublicKey, it.postingPublicKey)
                values.put(activePublicKey, it.activePublicKey)
                values.put(votingPower, it.votingPower)
                values.put(location, it.location)
                values.put(website, it.website)
                values.put(registrationDate, it.registrationDate)
                values.put(userCover, it.userCover)
                values.put(lastTimeInfoUpdatedAt, it.lastTimeInfoUpdatedAt)
                values.put(showName, it.shownName)
                writableDatabase.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }

        fun getGolosUsersAccountInfo(writableDatabase: SQLiteDatabase?): List<GolosUserAccountInfo> {
            val out = arrayListOf<GolosUserAccountInfo>()
            writableDatabase ?: return out
            val c = writableDatabase.rawQuery("select * from $databaseName", emptyArray())

            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    out.add(GolosUserAccountInfo(
                            c.getString(userName).orEmpty(),
                            c.getString(avatarPath),
                            c.getString(userMotto),
                            c.getString(showName),
                            c.getLong(postsCount),
                            c.getDouble(accountWorth),
                            c.getDouble(gbgAmount),
                            c.getDouble(golosAmount),
                            c.getDouble(golosPower),
                            c.getDouble(safeGbg),
                            c.getDouble(safeGolos),
                            c.getInt(subscribersCount),
                            c.getInt(subscriptionsCount),
                            c.getString(postingPublicKey).orEmpty(),
                            c.getString(activePublicKey).orEmpty(),
                            c.getInt(votingPower),
                            c.getString(location).orEmpty(),
                            c.getString(website).orEmpty(),
                            c.getLong(registrationDate),
                            c.getString(userCover),
                            c.getLong(lastTimeInfoUpdatedAt)

                    ))
                    c.moveToNext()
                }
            }
            c.close()
            return out

        }


    }


    private object DiscussionItemsTable {
        const val databaseName = "discussion_item_table"
        private const val url = "url"
        private const val title = "title"
        private const val id = "id"
        private const val categoryName = "categoryName"
        private const val tags = "tags"
        private const val images = "images"
        private const val links = "links"
        private const val votesNum = "votesNum"
        private const val votesRshares = "votesRshares"
        private const val commentsCount = "commentsCount"
        private const val permlink = "permlink"
        private const val gbgAmount = "gbgAmount"
        private const val netRshares = "netRshares"
        private const val pendingPayoutValue = "pendingPayoutValue"
        private const val totalPayoutValue = "totalPayoutValue"
        private const val curatorPayoutValue = "curatorPayoutValue"
        private const val body = "body"
        private const val author = "author"
        private const val format = "format"
        private const val parentPermlink = "parentPermlink"
        private const val level = "level"
        private const val gbgCostInDollars = "gbgCostInDollars"
        private const val reputation = "reputation"
        private const val lastUpdated = "lastUpdated"
        private const val created = "created"
        private const val isUserUpvotedOnThis = "isUserUpvotedOnThis"
        private const val type = "type"
        private const val reblogged = "reblogged"
        private const val cleanedFromImages = "cleanedFromImages"
        private const val childrenCount = "childrenCount"
        private const val upvotes = "upvotes"
        private const val downvotes = "downvotes"
        private const val bodyLength = "bodyLength"
        private const val parentAuthor = "parentAuthor"


        const val createTableString = "create table if not exists $databaseName ( $id integer primary key ," +
                "$url text, $title text, $categoryName text, $tags text, $images text, $links text," +
                "$votesNum integer, $votesRshares integer, $commentsCount integer, $pendingPayoutValue integer," +
                "$permlink text, $gbgAmount real, $body text, $author text, $format text, $parentPermlink text," +
                "$level integer, $gbgCostInDollars real, $reputation integer, $lastUpdated integer, $curatorPayoutValue integer," +
                "$created integer, $isUserUpvotedOnThis integer, $type text, $totalPayoutValue integer," +
                "$cleanedFromImages text, $childrenCount integer, $bodyLength integer, $parentAuthor text, $upvotes integer," +
                "$downvotes integer, $reblogged text, $netRshares integer)"

        fun save(items: List<GolosDiscussionItem>,
                 voteTable: VotesTable,
                 db: SQLiteDatabase) {

            if (items.isEmpty()) return
            var savingItems = items
            if (savingItems.size > 200) savingItems = savingItems.subList(0, 200)

            val values = ContentValues()
            db.beginTransaction()
            val votestToSave = hashMapOf<Long, List<VoteLight>>()
            savingItems.forEach {
                values.put(this.id, it.id)
                values.put(this.url, it.url)
                values.put(this.title, it.title)
                values.put(this.categoryName, it.categoryName)
                values.put(this.tags, mapper.writeValueAsString(it.tags))
                values.put(this.images, mapper.writeValueAsString(it.images))
                values.put(this.links, mapper.writeValueAsString(it.links))
                values.put(this.permlink, it.permlink)
                values.put(this.gbgAmount, it.gbgAmount)
                values.put(this.body, if (it.body.length > 1024) it.body.substring(0, 1024) else it.body)
                values.put(this.author, it.author)
                values.put(this.format, it.format.name)
                values.put(this.parentPermlink, it.parentPermlink)
                values.put(this.level, it.level)
                values.put(this.reputation, it.reputation)
                values.put(this.pendingPayoutValue, it.pendingPayoutValue)
                values.put(this.totalPayoutValue, it.totalPayoutValue)
                values.put(this.curatorPayoutValue, it.curatorPayoutValue)
                values.put(this.lastUpdated, it.lastUpdated)
                values.put(this.created, it.created)
                values.put(this.type, it.type.name)
                values.put(this.bodyLength, it.bodyLength)
                values.put(this.cleanedFromImages, it.cleanedFromImages)
                values.put(this.childrenCount, it.childrenCount)
                values.put(this.votesNum, it.votesNum)
                values.put(this.upvotes, it.upvotesNum)
                values.put(this.downvotes, it.downvotesNum)
                values.put(this.netRshares, it.netRshares)
                values.put(this.votesRshares, it.votesRshares)
                values.put(this.commentsCount, it.commentsCount)
                values.put(this.parentAuthor, it.parentAuthor)
                values.put(this.reblogged, it.rebloggedBy)

                votestToSave.put(it.id, it.activeVotes)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            voteTable.save(votestToSave, db)

            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun get(ids: List<Long>,

                db: SQLiteDatabase): List<GolosDiscussionItem> {
            if (ids.isEmpty()) return listOf()

            val workingIds = if (ids.size > 100) ids.subList(0, 100) else ids

            val cursor = db.rawQuery("select * from $databaseName where $id in ${ids.joinToString(prefix = "(", postfix = ")")}", null)
            val out = ArrayList<GolosDiscussionItem>(cursor.count)

            if (cursor.count != 0) {
                cursor.moveToFirst()
                val temp = ArrayList<GolosDiscussionItem>(cursor.count)
                val stringListType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)



                while (!cursor.isAfterLast) {

                    val tags = mapper.readValue<List<String>>(cursor.getString(tags)
                            ?: "", stringListType).toArrayList()
                    val images = mapper.readValue<List<String>>(cursor.getString(images)
                            ?: "", stringListType).toArrayList()
                    val links = mapper.readValue<List<String>>(cursor.getString(links)
                            ?: "", stringListType).toArrayList()


                    val format = cursor.getString(format)
                    val itemType = cursor.getString(type)
                    val discussionItem = GolosDiscussionItem(cursor.getString(url) ?: "",
                            cursor.getLong(id),
                            cursor.getString(title) ?: "",
                            cursor.getString(categoryName) ?: "",
                            tags,
                            images,
                            links,
                            cursor.getInt(votesNum),
                            cursor.getInt(upvotes),
                            cursor.getInt(downvotes),
                            cursor.getLong(votesRshares),
                            cursor.getLong(netRshares),
                            cursor.getInt(commentsCount),
                            cursor.getString(permlink) ?: "",
                            cursor.getDouble(gbgAmount),
                            cursor.getDouble(pendingPayoutValue),
                            cursor.getDouble(totalPayoutValue),
                            cursor.getDouble(curatorPayoutValue),
                            cursor.getString(body) ?: "",
                            cursor.getLong(bodyLength),
                            cursor.getString(author) ?: "",
                            cursor.getString(this.reblogged),
                            if (format != null) GolosDiscussionItem.Format.valueOf(format) else GolosDiscussionItem.Format.HTML,
                            arrayListOf(),
                            cursor.getString(parentPermlink) ?: "",
                            cursor.getString(parentAuthor) ?: "",
                            cursor.getInt(childrenCount),
                            cursor.getInt(level),
                            cursor.getLong(reputation),
                            cursor.getLong(lastUpdated),
                            cursor.getLong(created),
                            arrayListOf(),
                            if (itemType != null) GolosDiscussionItem.ItemType.valueOf(itemType) else GolosDiscussionItem.ItemType.PLAIN,
                            cursor.getString(cleanedFromImages) ?: "",
                            arrayListOf())


                    temp.add(discussionItem)
                    cursor.moveToNext()
                }
                workingIds.forEach { id ->
                    val item = temp.find { it.id == id }
                    item?.let { out.add(item) }
                }
            }
            cursor.close()
            return out
        }

        fun getAll(
                db: SQLiteDatabase): Map<Long, GolosDiscussionItem> {

            val cursor = db.rawQuery("select * from $databaseName", null)
            val out = ArrayList<GolosDiscussionItem>(cursor.count)



            if (cursor.count != 0) {
                cursor.moveToFirst()

                val stringListType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)

                while (!cursor.isAfterLast) {

                    val tags = mapper.readValue<List<String>>(cursor.getString(tags)
                            ?: "", stringListType).toArrayList()
                    val images = mapper.readValue<List<String>>(cursor.getString(images)
                            ?: "", stringListType).toArrayList()
                    val links = mapper.readValue<List<String>>(cursor.getString(links)
                            ?: "", stringListType).toArrayList()


                    val format = cursor.getString(format)
                    val itemType = cursor.getString(type)
                    val voteType = cursor.getInt(isUserUpvotedOnThis)
                    val author = cursor.getString(author)

                    val discussionItem = GolosDiscussionItem(cursor.getString(url) ?: "",
                            cursor.getLong(id),
                            cursor.getString(title) ?: "",
                            cursor.getString(categoryName) ?: "",
                            tags,
                            images,
                            links,
                            cursor.getInt(votesNum),
                            cursor.getInt(upvotes),
                            cursor.getInt(downvotes),
                            cursor.getLong(votesRshares),
                            cursor.getLong(netRshares),
                            cursor.getInt(commentsCount),
                            cursor.getString(permlink) ?: "",
                            cursor.getDouble(gbgAmount),
                            cursor.getDouble(pendingPayoutValue),
                            cursor.getDouble(totalPayoutValue),
                            cursor.getDouble(curatorPayoutValue),
                            cursor.getString(body) ?: "",
                            cursor.getLong(bodyLength),
                            author.orEmpty(),
                            cursor.getString(reblogged),
                            if (format != null) GolosDiscussionItem.Format.valueOf(format) else GolosDiscussionItem.Format.HTML,

                            arrayListOf(),
                            cursor.getString(parentPermlink) ?: "",
                            cursor.getString(parentAuthor) ?: "",
                            cursor.getInt(childrenCount),
                            cursor.getInt(level),
                            cursor.getLong(reputation),
                            cursor.getLong(lastUpdated),
                            cursor.getLong(created),
                            arrayListOf(),
                            if (itemType != null) GolosDiscussionItem.ItemType.valueOf(itemType) else GolosDiscussionItem.ItemType.PLAIN,
                            cursor.getString(cleanedFromImages) ?: "",
                            arrayListOf())


                    out.add(discussionItem)
                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out.associateBy { it.id }
        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }

    }

    private object StoriesRequestsTable {
        val databaseName = "stories_filter_table"
        private val serializedRequest = "json"

        val createTableString = "create table if not exists $databaseName ( $serializedRequest text primary key)"

        fun save(filters: List<FilteredStoriesIds>, db: SQLiteDatabase) {
            if (filters.isEmpty()) return
            val values = ContentValues()
            db.beginTransaction()
            filters.forEach {
                values.put(serializedRequest, mapper.writeValueAsString(it))

                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun get(db: SQLiteDatabase): List<FilteredStoriesIds> {
            val cursor = db.rawQuery("select * from $databaseName", null)
            val out = ArrayList<FilteredStoriesIds>(cursor.count)
            if (cursor.count != 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    out.add(mapper.readValue(cursor.getString(serializedRequest) ?: "",
                            FilteredStoriesIds::class.java))
                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out

        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }
    }


    private object VotesTable {
        val databaseName = "votes_table"
        private val userName = "name"
        private val rshares = "rshares"
        private val percent = "percent"
        private val storyId = "storyId"
        val createTableString = "create table if not exists $databaseName ( $userName text ," +
                "$rshares integer, $percent integer, $storyId integer )"

        fun save(votes: List<VoteLight>, storyId: Long, db: SQLiteDatabase) {
            if (votes.isEmpty()) return
            val values = ContentValues()
            db.beginTransaction()
            votes.forEach {
                values.put(userName, it.name)
                values.put(rshares, it.rshares)
                values.put(percent, it.percent)
                values.put(this.storyId, storyId)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun save(votes: Map<Long, List<VoteLight>>, db: SQLiteDatabase) {
            if (votes.isEmpty()) return
            val values = ContentValues()
            db.beginTransaction()
            votes.forEach {
                val votes = it.value
                val id = it.key
                votes.forEach {
                    values.put(userName, it.name)
                    values.put(rshares, it.rshares)
                    values.put(percent, it.percent)
                    values.put(this.storyId, id)
                    db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }


        fun get(storyId: Long, db: SQLiteDatabase): List<VoteLight> {

            val statement = "select * from $databaseName where ${this.storyId} = $storyId order by $rshares desc"
            val cursor = db.rawQuery(statement, null)
            val out = ArrayList<VoteLight>(cursor.count)

            if (cursor.count != 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    out.add(VoteLight(
                            cursor.getString(userName) ?: "",
                            cursor.getLong(rshares),
                            cursor.getInt(percent)
                    ))
                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out
        }

        fun getAll(db: SQLiteDatabase): Map<Long, List<VoteLight>> {

            val statement = "select * from $databaseName"
            val cursor = db.rawQuery(statement, null)
            val out = HashMap<Long, ArrayList<VoteLight>>(100)

            if (cursor.count != 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val vote = VoteLight(
                            cursor.getString(userName) ?: "",
                            cursor.getLong(rshares),
                            cursor.getInt(percent)
                    )
                    val id = cursor.getLong(storyId)
                    if (!out.containsKey(id)) out[id] = ArrayList(100)
                    out[id]?.add(vote)

                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out
        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }
    }

    private object UserFilterTable {
        const val databaseName = "filter_table"
        private val tagName = "name"
        private val filterName = "filter"
        val createTableString = "create table if not exists $databaseName ( $tagName text ," +
                "$filterName text )"

        fun save(tags: List<Tag>, filterName: String, db: SQLiteDatabase) {

            db.delete(databaseName, "${UserFilterTable.filterName} = \'$filterName\'", null)
            val values = ContentValues()
            db.beginTransaction()
            tags.forEach {
                values.put(tagName, it.name)
                values.put(this.filterName, filterName)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun get(filterName: String,
                tagsTable: TagsTable,
                db: SQLiteDatabase): List<Tag> {
            val cursor =
                    db.rawQuery(" select ${tagsTable.databaseName}.${tagsTable.tagName}, ${tagsTable.payoutInGbg}, ${tagsTable.votesCount},  ${tagsTable.topPostsCount}" +
                            " from ${tagsTable.databaseName} " +
                            " inner join $databaseName on $databaseName.$tagName = ${tagsTable.databaseName}.${tagsTable.tagName} " +
                            " where ${this.filterName} = \'$filterName\'", null)
            if (cursor.count == 0) return listOf()
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tagName = cursor.getString(TagsTable.tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(TagsTable.payoutInGbg),
                            cursor.getLong(TagsTable.votesCount),
                            cursor.getLong(TagsTable.topPostsCount))
                    out.add(tag)
                }

                cursor.moveToNext()
            }
            cursor.close()
            return out
        }

        fun getAll(db: SQLiteDatabase): List<String> {
            val cursor = db.query(databaseName, arrayOf(tagName),
                    "distinct", null, null, null, null)
            val out = ArrayList<String>()
            while (!cursor.isAfterLast) {
                val tagName = cursor.getString(filterName)
                if (tagName != null) {
                    out.add(tagName)
                }
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }
    }

    private object TagsTable {
        val databaseName = "tag"
        val tagName = "name"
        val payoutInGbg = "payoutInGbg"
        val votesCount = "votes"
        val topPostsCount = "topPostsCount"
        val createTableString = "create table if not exists $databaseName ( $tagName text primary key," +
                "$payoutInGbg real, $votesCount integer, $topPostsCount integer)"

        fun save(tags: List<Tag>, db: SQLiteDatabase) {
            if (tags.isEmpty()) return

            val values = ContentValues()
            db.beginTransaction()
            tags.forEach {
                values.put(tagName, it.name)
                values.put(payoutInGbg, it.payoutInGbg)
                values.put(votesCount, it.votes)
                values.put(topPostsCount, it.topPostsCount)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun getTagsFromDb(db: SQLiteDatabase, withName: List<String>) {
            var selectionArg = ""
            selectionArg.forEach {
                selectionArg += " $tagName = \"$it\""
            }
            val cursor = db.query(databaseName, arrayOf(tagName, payoutInGbg, votesCount, topPostsCount),
                    selectionArg, null, null, null, "$payoutInGbg desc")
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tagName = cursor.getString(tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(payoutInGbg),
                            cursor.getLong(votesCount),
                            cursor.getLong(topPostsCount))
                    out.add(tag)
                }

                cursor.moveToNext()
            }
            cursor.close()
        }

        fun get(db: SQLiteDatabase): List<Tag> {
            val cursor = db.query(databaseName, arrayOf(tagName, payoutInGbg, votesCount, topPostsCount),
                    null, null, null, null, "$payoutInGbg desc")
            if (cursor.count == 0) return listOf()
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tagName = cursor.getString(tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(payoutInGbg),
                            cursor.getLong(votesCount),
                            cursor.getLong(topPostsCount))
                    out.add(tag)
                }
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }
    }
}